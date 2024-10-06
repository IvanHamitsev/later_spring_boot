package ru.practicum.note;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
interface ItemNoteService {

    @Transactional
    ItemNoteDto addNewItemNote(long userId, ItemNoteDto itemNoteDto);

    List<ItemNoteDto> searchNotesByUrl(long userId, String url);

    List<ItemNoteDto> searchNotesByTag(long userId, String tag);

    List<ItemNoteDto> listAllItemsWithNotes(long userId, int from, int size);
}