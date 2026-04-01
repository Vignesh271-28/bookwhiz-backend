package com.example.BookWhiz.service;

import com.example.BookWhiz.dto.request.ShowRequest;
import com.example.BookWhiz.dto.response.ShowResponse;
import java.util.List;
import java.util.Optional;

public interface ShowService {
    List<ShowResponse> getAllShows();
    Optional<ShowResponse> findById(Long id);
    ShowResponse createShow(ShowRequest request);
    Optional<ShowResponse> updateShow(Long id, ShowRequest request);
    void deleteShow(Long id);
}