# Flash Trade Challenge

**Build the Fastest "One-Click" Trading App**

## 1. Overview

### **The Vision**

We are looking for talented developers to build a **mobile-only** application aimed at a **global user base**.

The goal is to create a mobile app that allows users to participate in the meme token market in the fastest, most user-friendly way possible. The target user is someone who wants to "get in on the action" but doesn't have time to track tokens, monitor prices, or perform complex sell operations.

### The Challenge concept:

The objective is to build a mobile app (iOS or Android) centered around a "one-click" user experience.

- **Format:** Open Challenge.
- **Program Duration:** 4 Weeks.
- **Target:** Mobile-only (Native or cross-platform).
- **Costs:** Participants are responsible for their own server/computing resource costs.

## 2. Minimum Viable Concept - “MVC”

This app must provide a few core functionalities that make the fastest user actions as possible:

1. **Wallet creation and wallet funding:** Make the onboarding process from when users download the app to when user’s wallet is funded as fast as possible.
2. **Buy and Sell tokens:** Be as creative as you can to minimize the needed time from when users open the app to buy/sell token is done. The app should allow users to be able to sell the token they bought after 24 hour automatically or manually, as long as it is fast.

## 3. Requirements

To be eligible for the **Base Reward**, the product **must** meet the following two core flow requirements:

| Flow | UX Requirement | Technical Requirement |
| --- | --- | --- |
| **Funding** | A user must be able to simply get funds *into* the app (regardless of amount)

Funding time: is measured from when users download the app until users successfully have funds to buy tokens. | Can use any sort of multisig, smart contract wallet or hot wallet…  |
| Fast buy
Sell after 24h | A user must be able to buy a token as fast as possible.

Action Time: is measured from when users open the app until users successfully buy a token.

After around 24h, the bought tokens should be sold at any price. | Fixed or User customized amounts are all welcomed. Any chains that Kyber supports: [https://docs.kyberswap.com/getting-started/supported-exchanges-and-networks](https://docs.kyberswap.com/getting-started/supported-exchanges-and-networks)

Buying/Selling must go through Kyber Aggregator. |

## 4. Rewards & Long-Term Collaboration

This is a merit-based program, not a winner-take-all competition.

- **Base Reward ($1,000):** **Every individual/team** that successfully completes the "MVC" requirements (Section 5) will receive $1,000.
- **Long-Term Collaboration Opportunity:** The best builders will be rewarded $5,000 and will be offered a chance for a long-term collaboration with Kyber to develop the new Kyber’s mobile app either by freelancer contract or joining the team as an official member.
- App’s ownership: The application in this challenge belongs to you and always will be, unless, specific agreement is formed between you and Kyber..

## 5. Provided Resources

Participants will be given access to the **KyberSwap API** (a test/challenge version). More information in this page: [https://docs.kyberswap.com/](https://docs.kyberswap.com/).

### API Details (Kyberswap Token List API)

This API will return a list of tokens and related info, with the following capabilities:

- **Endpoint (Assumed):** `GET /api/v1/tokens`
- **Core Features:**
    - **Pagination:** Support for `page` and `limit`.
    - **Filter:**
        - `category=newly_liquid`: (Priority) Filter for tokens that just received liquidity (crossed a certain threshold).
    - **Sort:**
        - `sortBy=createdAt` (Token creation time)
        - `sortBy=tvl` (TVL of the largest pool)
        - `sortBy=activeLiquidity`
        - `sortBy=volume`

## 6. Judging & Submission

- **MVC Judging (Pass/Fail):** A review panel will test the three MVC flows. If all three function correctly and meet the UX standard, you are eligible for the Base Reward.
- **How to Submit your profile:** Send an email to [hr@kyber.network](mailto:hr@kyber.network) with Subject: Flash Trade Challenge - Your Name, the email is supposed to contain:
    - Your profile (either CVs, github profile, or any form of showcase of your ability and passion)
    - Your ideas of how you will compete with others to make the app the fastest.

  **Top 5 strongest (and most passionate) profiles will be chosen to do this challenge.**

- **How to Submit your work:** We will let you know via emails later.
