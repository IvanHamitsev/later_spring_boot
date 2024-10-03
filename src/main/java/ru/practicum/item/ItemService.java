package ru.practicum.item;

import java.util.List;
import java.util.Set;

interface ItemService {
    List<ItemDto> getItems(long userId);

    List<ItemDto> getItems(long userId, Set<String> tags);

    ItemDto addNewItem(long userId, ItemDto itemDto);

    void deleteItem(long userId, long itemId);
}
