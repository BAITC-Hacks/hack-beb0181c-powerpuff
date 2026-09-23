package com.powerpuff.backend.service;

import com.powerpuff.backend.dto.CreateItemRequest;
import com.powerpuff.backend.dto.ItemDto;
import com.powerpuff.backend.exception.NotFoundException;
import com.powerpuff.backend.model.Item;
import com.powerpuff.backend.repository.ItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findAll() {
        return repository.findAll().stream().map(ItemDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ItemDto findById(Long id) {
        return repository.findById(id)
                .map(ItemDto::from)
                .orElseThrow(() -> new NotFoundException("Item " + id + " not found"));
    }

    @Transactional
    public ItemDto create(CreateItemRequest request) {
        Item saved = repository.save(new Item(request.title(), request.description()));
        return ItemDto.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Item " + id + " not found");
        }
        repository.deleteById(id);
    }
}
