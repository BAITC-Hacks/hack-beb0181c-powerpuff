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
    private static final List<String> KEYS=List.of("OBYEM","KOLICHESTVO_POLYUSOV","NOMINALNOE_NAPRYAZHENIE","NOMINALNYY_TOK","NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST","TIP_USTANOVKI","KHARAKTERISTIKA_SRABATYVANIYA");
    public Map<String,Object> find(long id){
        var source=products.get(id);var props=source.product().properties();
        if(props==null || !source.warnings().isEmpty() || !props.path("OBYEM").asText().equalsIgnoreCase("Автоматический выключатель")
                || KEYS.stream().anyMatch(k->!known(props.path(k).asText())))
            return response(source.product().quantity(), List.of(), "Недостаточно непротиворечивых обязательных характеристик для подбора.");
        var params=new ArrayList<Object>();params.add(id);
        var clauses=new ArrayList<String>();
        for(var key:KEYS){clauses.add("lower(properties->>?)=lower(?)");params.add(key);params.add(props.path(key).asText());}
        var ids=db.queryForList("SELECT id FROM products WHERE id<>? AND quantity>0 AND detail_error IS NULL AND "+String.join(" AND ",clauses)+" ORDER BY id LIMIT 20",Long.class,params.toArray());
        var results=new ArrayList<Object>();
        for(long candidate:ids){var p=products.get(candidate);if(p.warnings().isEmpty()) results.add(Map.of("product",p,"matchedCharacteristics",KEYS,"compatibilityStatus","UNCONFIRMED_CANDIDATE","explanation","Совпадают тип, полюса, напряжение, ток, отключающая способность, крепление и характеристика срабатывания. Это предварительный кандидат: габариты и применимость должен проверить специалист."));if(results.size()==5)break;}
        return response(source.product().quantity(), results, results.isEmpty()?"Подходящих кандидатов в загруженной выборке нет.":"Найдены предварительные кандидаты по семи характеристикам; остатки на момент синхронизации, доступность отгрузки не подтверждена.");
    }
    private boolean known(String value) {
        String normalized=value.strip().toLowerCase(Locale.ROOT);
        return !normalized.isBlank() && !Set.of("нет", "не указано", "неизвестно", "не уточнено", "n/a", "unknown", "-", "0").contains(normalized);
    }
    private Map<String,Object> response(java.math.BigDecimal stock, List<?> items, String reason) {
        String availability=stock==null?"Остаток исходного товара неизвестен. ":stock.signum()==0?"Товар отсутствует по данным последней синхронизации. ":"";
        return Map.of("items",items,"confirmed",false,"message",availability+"Подтверждённых аналогов не найдено. "+reason+" Уточните подбор у менеджера.");
    }
}
