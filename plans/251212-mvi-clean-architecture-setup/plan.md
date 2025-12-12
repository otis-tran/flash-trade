# MVI + Clean Architecture Setup Plan

## Overview
Setup foundational architecture layers for Flash Trade Android app following MVI (Model-View-Intent) + Clean Architecture principles.

**Plan ID:** 251212-mvi-clean-architecture-setup
**Created:** 2024-12-12
**Status:** Ready for Implementation

## Context
- **Project:** Flash Trade (one-click crypto trading app)
- **Current State:** ~10% complete (project setup done, dependencies configured)
- **Target:** Base architecture scaffolding with MVI + Clean Architecture

## Implementation Phases

| Phase | Name | Status | Files | Priority |
|-------|------|--------|-------|----------|
| 01 | Core Utils & Base MVI | Complete | 5 | High |
| 02 | Domain Layer | Complete | 8 | High |
| 03 | Data Layer - Local | Pending | 6 | High |
| 04 | Data Layer - Remote | Pending | 4 | Medium |
| 05 | DI Modules | Pending | 4 | High |
| 06 | Presentation Base | Pending | 4 | Medium |

## Phase Details

### Phase 01: Core Utils & Base MVI
[phase-01-core-utils-base-mvi.md](phase-01-core-utils-base-mvi.md)
- Result wrapper class
- Base MVI interfaces (Intent, State)
- MviContainer for state management

### Phase 02: Domain Layer
[phase-02-domain-layer.md](phase-02-domain-layer.md)
- Domain entities (Token, Trade, Wallet, User)
- Repository interfaces
- Base UseCase class

### Phase 03: Data Layer - Local
[phase-03-data-layer-local.md](phase-03-data-layer-local.md)
- Room Database setup
- DAOs and Entities
- DataStore preferences

### Phase 04: Data Layer - Remote
[phase-04-data-layer-remote.md](phase-04-data-layer-remote.md)
- API service interfaces (placeholder)
- DTO structures
- Mapper utilities

### Phase 05: DI Modules
[phase-05-di-modules.md](phase-05-di-modules.md)
- FlashTradeApplication with @HiltAndroidApp
- Network, Database, Repository modules
- Update MainActivity with @AndroidEntryPoint

### Phase 06: Presentation Base
[phase-06-presentation-base.md](phase-06-presentation-base.md)
- Navigation graph setup
- Common UI components
- Base screen scaffolding

## Dependencies
```
Phase 01 → Phase 02 → Phase 03 → Phase 04
                  ↘        ↘
                   Phase 05 → Phase 06
```

## Success Criteria
- [ ] All packages created per architecture spec
- [ ] Hilt DI compiles without errors
- [ ] Room database initializes
- [ ] App builds and runs
- [ ] File size <200 lines each

## Files to Create (~31 files)

### Root Package
- `FlashTradeApplication.kt`

### util/
- `Result.kt`

### presentation/base/
- `MviIntent.kt`, `MviState.kt`, `MviContainer.kt`

### presentation/navigation/
- `NavGraph.kt`, `Screen.kt`

### presentation/common/
- `LoadingIndicator.kt`

### domain/model/
- `Token.kt`, `Trade.kt`, `Wallet.kt`, `User.kt`

### domain/repository/
- `TradeRepository.kt`, `WalletRepository.kt`, `UserRepository.kt`

### domain/usecase/
- `UseCase.kt`

### data/local/database/
- `FlashTradeDatabase.kt`

### data/local/entity/
- `TradeEntity.kt`, `WalletEntity.kt`

### data/local/dao/
- `TradeDao.kt`, `WalletDao.kt`

### data/local/datastore/
- `UserPreferences.kt`

### data/remote/kyber/
- `KyberApiService.kt`

### data/repository/
- `TradeRepositoryImpl.kt`, `WalletRepositoryImpl.kt`

### di/
- `AppModule.kt`, `NetworkModule.kt`, `DatabaseModule.kt`, `RepositoryModule.kt`

## Notes
- Keep all files <200 lines
- Domain layer = pure Kotlin (no Android deps)
- Placeholder implementations where needed
- Focus on structure, not feature logic
