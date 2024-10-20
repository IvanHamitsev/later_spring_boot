package ru.practicum.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ItemServiceImpl implements ItemService {
    private final ItemRepository repository;
    private final UserRepository userRepository;
    private final UrlMetadataRetriever retriever;

    @Override
    public List<ItemDto> getItems(long userId) {
        List<Item> userItems = repository.findByUserId(userId);
        return ItemMapper.mapToItemDto(userItems);
    }

    @Override
    public List<ItemDto> getItems(long userId, Set<String> tags) {
        BooleanExpression byUserId = QItem.item.user.id.eq(userId);
        BooleanExpression byAnyTag = QItem.item.tags.any().in(tags);
        Iterable<Item> foundItems = repository.findAll(byUserId.and(byAnyTag));
        // а можно собрать в List с помощью Guava
        //List<Item> itemList = Lists.newArrayList(repository.findAll(byUserId));
        return ItemMapper.mapToItemDto(foundItems);
    }

    @Override
    @Transactional
    public ItemDto addNewItem(long userId, ItemDto itemDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = ItemMapper.mapToItem(itemDto, user);
        // получить метаданные по ссылке
        var urlMetadata = retriever.retrieve(item.getUrl());
        // если уже есть в базе, нужно только обновить
        List<Item> existingItems = repository.findByResolvedUrl(urlMetadata.getResolvedUrl());
        if (existingItems.isEmpty()) {
            // В новый Item надо добавить отдельные поля интерфейса urlMetadata
            item.setResolvedUrl(urlMetadata.getResolvedUrl());
            item.setMimeType(urlMetadata.getMimeType());
            item.setTitle(urlMetadata.getTitle());
            item.setHasImage(urlMetadata.isHasImage());
            item.setHasVideo(urlMetadata.isHasVideo());
            item.setDateResolved(urlMetadata.getDateResolved());
        } else {
            if (existingItems.size() > 1) {
                throw new RuntimeException("Нарушение целостности БД: уже существует более одной resolvedUrl "
                        + urlMetadata.getResolvedUrl());
            }
            Item existingItem = existingItems.getFirst();
            // добавить к существующим ссылкам тэги, которые пользователь указал для новой, если они есть
            var tags = existingItem.getTags();
            tags.addAll(item.getTags());
            existingItem.setTags(tags);
            item = existingItem;
        }
        item = repository.save(item);
        return ItemMapper.mapToItemDto(item);
    }

    public ItemDto replaseTagOfItem(long userId, long itemId, String oldTag, String newTag) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = repository.findById(itemId)
                .orElseThrow(() -> new RuntimeException(String.format("Item with id %d not found", itemId)));
        if (item.getTags().contains(oldTag)) {
            item.getTags().remove(oldTag);
            item.getTags().add(newTag);
            return ItemMapper.mapToItemDto(repository.save(item));
        } else {
            throw new RuntimeException(String.format("Item with id %d don't have tag %s", itemId, oldTag));
        }
    }

    @Override
    @Transactional
    public void deleteItem(long userId, long itemId) {
        repository.deleteByUserIdAndId(userId, itemId);
    }
}
