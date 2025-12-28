# Portfolio Files Scout Report

## Summary
Found 31 portfolio-related source files across presentation, domain, and data layers. Core portfolio feature is in MVI architecture with 5 presentation files and supporting domain models, repositories, and API layers.

## Portfolio Feature (Presentation Layer)
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\presentation\feature\portfolio\PortfolioViewModel.kt` - MVI viewmodel managing portfolio state, network changes, wallet loading, refresh
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\presentation\feature\portfolio\PortfolioState.kt` - Portfolio UI state + domain models (TokenHolding, Transaction, PriceChanges, Timeframe, TransactionType, TransactionStatus)
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\presentation\feature\portfolio\PortfolioEvent.kt` - User interaction events
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\presentation\feature\portfolio\PortfolioEffect.kt` - Side effects (clipboard, navigator, toast)
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\presentation\feature\portfolio\PortfolioScreen.kt` - Compose UI

## Domain Models
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\Token.kt` - Token definition with TVL, liquidity, safety checks
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\User.kt` - User profile model
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\UserAuthState.kt` - Auth state with wallet address
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\AuthState.kt` - Auth status enum
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\Settings.kt` - User settings including network mode
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\Quote.kt` - Price quote data
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\SwapResult.kt` - Trade result
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\model\EncodedSwap.kt` - Encoded swap transaction

## Repositories
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\repository\AuthRepository.kt` - Interface for auth operations (getUserAuthState)
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\repository\AuthRepositoryImpl.kt` - Auth implementation
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\repository\SettingsRepository.kt` - Interface for settings (observeSettings)
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\repository\SettingsRepositoryImpl.kt` - Settings implementation
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\repository\TokenRepository.kt` - Token list operations
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\repository\TokenRepositoryImpl.kt` - Token implementation
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\repository\SwapRepository.kt` - Swap operations
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\repository\SwapRepositoryImpl.kt` - Swap implementation

## Network/API Layer
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\api\KyberApiService.kt` - Token list API
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\api\KyberSwapApiService.kt` - Swap/route API
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\BaseResponse.kt` - Response wrapper
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\kyber\TokenListResponse.kt` - Kyber token list
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\kyber\RouteResponseDto.kt` - Route response
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\kyber\BuildRouteRequestDto.kt` - Route request
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\kyber\BuildRouteResponseDto.kt` - Build route response
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\request\AuthRequest.kt` - Auth DTO
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\remote\dto\AuthDto.kt` - Auth response

## Mappers
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\mapper\TokenMapper.kt` - DTO to domain Token
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\mapper\UserMapper.kt` - User mapping
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\data\mapper\SwapMapper.kt` - Swap result mapping

## DI Modules
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\AppModule.kt` - App-level bindings
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\AuthModule.kt` - Auth dependencies
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\TokenModule.kt` - Token/portfolio dependencies
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\SettingsModule.kt` - Settings dependencies
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\SwapModule.kt` - Swap dependencies
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\NetworkModule.kt` - Network client setup
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\di\PrivyProvider.kt` - Privy SDK provider

## Core Components
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\core\base\BaseViewModel.kt` - MVI base class
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\core\base\UiState.kt` - State interface
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\core\base\UiEvent.kt` - Event interface
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\core\base\UiEffect.kt` - Effect interface
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\core\datastore\UserPreferences.kt` - DataStore for persistent preferences
- `D:\projects\flash-trade\app\src\main\java\com\otistran\flash_trade\domain\usecase\UseCase.kt` - Use case base class
