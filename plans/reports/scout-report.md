# Network Selection Functionality - Scout Report

## Summary
Found comprehensive network selection implementation in the Flash Trade Android app with two main components:
1. Network Selector Bottom Sheet for switching between blockchain networks
2. Network Mode Toggle for switching between mainnet/testnet

## Key Files Found

### Core Network Selection Components
- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/core/ui/components/NetworkSelectorBottomSheet.kt`**
  - Main bottom sheet UI for selecting blockchain networks
  - Displays networks with icons, names, chain IDs, and native tokens
  - Supports all major networks: Ethereum, Base, Arbitrum, Optimism, Polygon, BSC

- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/settings/components/NetworkModeSection.kt`**
  - Toggle switch for mainnet/testnet mode in settings
  - Shows warning chip when mainnet is selected
  - Includes descriptive text for each mode

- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/settings/components/NetworkConfirmDialog.kt`**
  - Confirmation dialog when switching to mainnet
  - Warning about real money transactions

### Data Models
- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/portfolio/PortfolioState.kt`**
  - Contains `Network` enum with network definitions
  - Each network includes: displayName, symbol, chainId, iconColor, explorerUrl
  - Networks: ETHEREUM, BASE, ARBITRUM, OPTIMISM, POLYGON, BSC

- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/domain/model/Settings.kt`**
  - Contains `NetworkMode` enum for mainnet/testnet
  - `Settings` data class includes networkMode preference

### Usage in UI
- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/portfolio/PortfolioScreen.kt`**
  - Network selector button in BalanceCard (top of portfolio)
  - Shows currently selected network with colored dot indicator
  - Opens NetworkSelectorBottomSheet on click

### Event Handling
- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/portfolio/PortfolioEvent.kt`**
  - Events: ToggleNetworkSelector, SelectNetwork

- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/portfolio/PortfolioViewModel.kt`**
  - Handles network selection state changes
  - Updates selected network in PortfolioState

### Settings Integration
- **`/d/projects/flash-trade/app/src/main/java/com/otistran/flash_trade/presentation/feature/settings/SettingsViewModel.kt`**
  - Handles NetworkMode (mainnet/testnet) changes
  - Integrates with SettingsRepository

## Architecture Notes
- Network selection is split into two concepts:
  1. **Network**: Individual blockchain networks (Ethereum, Base, etc.)
  2. **NetworkMode**: Environment mode (mainnet/testnet)
- Network selector is prominently displayed in the portfolio screen
- Network mode toggle is in settings for safety
- Confirmation required for mainnet switching
- Network preferences persisted via SettingsRepository

## Unresolved Questions
- How is the selected network used for actual transactions?
- Is network state synchronized across the app?
- Are there network-specific features or restrictions?
- How does the app handle network failures or switching issues?
