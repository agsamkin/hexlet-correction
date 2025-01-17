package io.hexlet.typoreporter.service;

import io.hexlet.typoreporter.domain.account.Account;
import io.hexlet.typoreporter.domain.workspace.AccountRole;
import io.hexlet.typoreporter.domain.workspace.Workspace;
import io.hexlet.typoreporter.domain.workspace.WorkspaceRole;
import io.hexlet.typoreporter.domain.workspace.WorkspaceRoleId;
import io.hexlet.typoreporter.domain.workspacesettings.WorkspaceSettings;
import io.hexlet.typoreporter.repository.AccountRepository;
import io.hexlet.typoreporter.repository.WorkspaceRepository;
import io.hexlet.typoreporter.repository.WorkspaceSettingsRepository;
import io.hexlet.typoreporter.service.dto.workspace.CreateWorkspace;
import io.hexlet.typoreporter.service.dto.workspace.WorkspaceInfo;
import io.hexlet.typoreporter.service.mapper.WorkspaceMapper;
import io.hexlet.typoreporter.web.exception.WorkspaceAlreadyExistException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository repository;

    private final WorkspaceSettingsRepository settingsRepository;

    private final WorkspaceMapper workspaceMapper;

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceInfo> getAllWorkspacesInfo() {
        return repository.findAll()
            .stream()
            .map(workspaceMapper::toWorkspaceInfo)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<WorkspaceInfo> getWorkspaceInfoByName(final String wksName) {
        return repository.getWorkspaceByName(wksName)
            .map(workspaceMapper::toWorkspaceInfo);
    }

    @Transactional(readOnly = true)
    public boolean existsWorkspaceByName(final String wksName) {
        return repository.existsWorkspaceByName(wksName);
    }

    @Transactional(readOnly = true)
    public boolean existsWorkspaceByUrl(final String wksUrl) {
        return repository.existsWorkspaceByName(wksUrl);
    }

    @Transactional
    public WorkspaceInfo createWorkspace(final CreateWorkspace createWks, final String userName) {
        if (repository.existsWorkspaceByName(createWks.name())) {
            throw new WorkspaceAlreadyExistException("name", createWks.name());
        }
        if (repository.existsWorkspaceByUrl(createWks.url())) {
            throw new WorkspaceAlreadyExistException("url", createWks.url());
        }

        final var wksToCreate = requireNonNull(workspaceMapper.toWorkspace(createWks));
        final var wksSettings = new WorkspaceSettings();
        wksSettings.setApiAccessToken(UUID.randomUUID());
        wksSettings.setWorkspace(wksToCreate);

        Account account = accountRepository.findAccountByUsername(userName).orElseThrow();

        final var workspaceRoleId = new WorkspaceRoleId(wksToCreate.getId(), account.getId());
        final var workspaceRole = new WorkspaceRole(workspaceRoleId, AccountRole.ROLE_ADMIN, wksToCreate, account);
        wksToCreate.addWorkspaceRole(workspaceRole);

        settingsRepository.save(wksSettings);
        return workspaceMapper.toWorkspaceInfo(wksToCreate);
    }

    @Transactional
    public Optional<WorkspaceInfo> updateWorkspace(final CreateWorkspace updateWks, final String oldWksName) {
        if (!oldWksName.equals(updateWks.name()) && repository.existsWorkspaceByName(updateWks.name())) {
            throw new WorkspaceAlreadyExistException("name", updateWks.name());
        }
        //TODO add update wks url, need check if it's not the same url and if it doesn't exist
        return repository.getWorkspaceByName(oldWksName)
            .map(oldWks -> oldWks.setName(updateWks.name()))
            .map(oldWks -> oldWks.setDescription(updateWks.description()))
            .map(repository::save)
            .map(workspaceMapper::toWorkspaceInfo);
    }

    @Transactional
    public Integer deleteWorkspaceByName(final String wksName) {
        return repository.deleteWorkspaceByName(wksName);
    }

    @Transactional(readOnly = true)
    public Optional<Workspace> getWorkspaceByName(final String name) {
        return repository.getWorkspaceByName(name);
    }
}
