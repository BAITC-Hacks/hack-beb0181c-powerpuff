package com.powerpuff.backend.service;

import com.powerpuff.backend.repository.StoredCatalogRepository;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalogService {
    private final JdbcTemplate db; private final StoredCatalogRepository products;
    public AnalogService(JdbcTemplate db,StoredCatalogRepository products){this.db=db;this.products=products;}
    // Консервативная демонстрационная группа автоматов: совпадение всех данных полей.
    private static final List<String> KEYS=List.of("OBYEM","KOLICHESTVO_POLYUSOV","NOMINALNOE_NAPRYAZHENIE","NOMINALNYY_TOK","NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST","TIP_USTANOVKI");
    public Map<String,Object> find(long id){
        var source=products.get(id);var props=source.product().properties();
        if(props==null || !source.warnings().isEmpty() || !props.path("OBYEM").asText().equalsIgnoreCase("Автоматический выключатель")
                || KEYS.stream().anyMatch(k->props.path(k).asText().isBlank()))
            return Map.of("items",List.of(),"message","Недостаточно непротиворечивых характеристик для подбора. Уточните у менеджера.");
        var params=new ArrayList<Object>();params.add(id);
        var clauses=new ArrayList<String>();
        for(var key:KEYS){clauses.add("lower(properties->>?)=lower(?)");params.add(key);params.add(props.path(key).asText());}
        var ids=db.queryForList("SELECT id FROM products WHERE id<>? AND quantity>0 AND detail_error IS NULL AND "+String.join(" AND ",clauses)+" ORDER BY id LIMIT 20",Long.class,params.toArray());
        var results=new ArrayList<Object>();
        for(long candidate:ids){var p=products.get(candidate);if(p.warnings().isEmpty()) results.add(Map.of("product",p,"matchedCharacteristics",KEYS,"explanation","Совпадают тип, полюса, напряжение, ток, отключающая способность и крепление. Требуется проверка габаритов и совместимости специалистом."));if(results.size()==5)break;}
        return Map.of("items",results,"message",results.isEmpty()?"Подходящих кандидатов в загруженной выборке нет.":"Кандидаты по характеристикам каталога; остатки на момент синхронизации.");
    }
}
