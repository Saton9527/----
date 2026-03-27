package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.ContestCreateRequest;
import com.acmtrain.backend.dto.ContestResponse;
import com.acmtrain.backend.dto.ProblemsetCreateRequest;
import com.acmtrain.backend.dto.ProblemsetResponse;
import com.acmtrain.backend.dto.UpdateProblemsetSolvedRequest;
import com.acmtrain.backend.entity.ContestLinkEntity;
import com.acmtrain.backend.entity.ProblemsetLinkEntity;
import com.acmtrain.backend.entity.ProblemsetProgressEntity;
import com.acmtrain.backend.repository.ContestLinkRepository;
import com.acmtrain.backend.repository.ProblemsetLinkRepository;
import com.acmtrain.backend.repository.ProblemsetProgressRepository;
import com.acmtrain.backend.service.dto.DtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResourceLinkService {

    private final ProblemsetLinkRepository problemsetLinkRepository;
    private final ProblemsetProgressRepository problemsetProgressRepository;
    private final ContestLinkRepository contestLinkRepository;

    public ResourceLinkService(
            ProblemsetLinkRepository problemsetLinkRepository,
            ProblemsetProgressRepository problemsetProgressRepository,
            ContestLinkRepository contestLinkRepository
    ) {
        this.problemsetLinkRepository = problemsetLinkRepository;
        this.problemsetProgressRepository = problemsetProgressRepository;
        this.contestLinkRepository = contestLinkRepository;
    }

    public List<ProblemsetResponse> getProblemsets(Long userId) {
        List<ProblemsetLinkEntity> links = problemsetLinkRepository.findAllByOrderByIdDesc();
        Map<Long, ProblemsetProgressEntity> progressMap = problemsetProgressRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(ProblemsetProgressEntity::getProblemsetId, Function.identity(), (left, right) -> right));

        return links.stream()
                .map(link -> {
                    ProblemsetProgressEntity progress = progressMap.get(link.getId());
                    boolean solved = progress != null && Boolean.TRUE.equals(progress.getSolved());
                    LocalDateTime solvedAt = solved ? progress.getUpdatedAt() : null;
                    return DtoMapper.toProblemsetResponse(link, solved, solvedAt);
                })
                .toList();
    }

    @Transactional
    public ProblemsetResponse createProblemset(Long userId, ProblemsetCreateRequest request) {
        String url = request.url().trim();
        if (!isLuoguUrl(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题单链接必须来自 luogu.com.cn");
        }

        ProblemsetLinkEntity entity = new ProblemsetLinkEntity();
        entity.setPlatform("LUOGU");
        entity.setUrl(url);
        entity.setTitle(defaultTitle(request.title(), "洛谷题单"));
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());

        return DtoMapper.toProblemsetResponse(problemsetLinkRepository.save(entity));
    }

    @Transactional
    public ProblemsetResponse updateProblemsetSolvedStatus(Long userId, Long problemsetId, UpdateProblemsetSolvedRequest request) {
        ProblemsetLinkEntity link = problemsetLinkRepository.findById(problemsetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题单不存在"));

        ProblemsetProgressEntity progress = problemsetProgressRepository.findByUserIdAndProblemsetId(userId, problemsetId)
                .orElseGet(() -> {
                    ProblemsetProgressEntity entity = new ProblemsetProgressEntity();
                    entity.setUserId(userId);
                    entity.setProblemsetId(problemsetId);
                    return entity;
                });

        boolean solved = Boolean.TRUE.equals(request.solved());
        progress.setSolved(solved);
        progress.setUpdatedAt(LocalDateTime.now());
        ProblemsetProgressEntity saved = problemsetProgressRepository.save(progress);

        return DtoMapper.toProblemsetResponse(link, solved, solved ? saved.getUpdatedAt() : null);
    }

    public List<ContestResponse> getContests() {
        return contestLinkRepository.findAllByOrderByIdDesc()
                .stream()
                .map(DtoMapper::toContestResponse)
                .toList();
    }

    @Transactional
    public ContestResponse createContest(Long userId, ContestCreateRequest request) {
        String url = request.url().trim();
        if (!isQojContestUrl(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "训练赛链接必须来自 qoj.ac/contest");
        }

        ContestLinkEntity entity = new ContestLinkEntity();
        entity.setPlatform("QOJ");
        entity.setUrl(url);
        entity.setTitle(defaultTitle(request.title(), "QOJ 训练赛"));
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());

        return DtoMapper.toContestResponse(contestLinkRepository.save(entity));
    }

    private String defaultTitle(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        return input.trim();
    }

    private boolean isLuoguUrl(String rawUrl) {
        URI uri = parseUri(rawUrl);
        String host = uri.getHost();
        return host != null && host.toLowerCase().contains("luogu.com.cn");
    }

    private boolean isQojContestUrl(String rawUrl) {
        URI uri = parseUri(rawUrl);
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null) {
            return false;
        }
        return host.equalsIgnoreCase("qoj.ac") && path.startsWith("/contest");
    }

    private URI parseUri(String url) {
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Invalid scheme");
            }
            return uri;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "链接格式不正确，需要 http/https 完整地址");
        }
    }
}