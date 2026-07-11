# Data Safety, Privacy, and Families Notes

This document is a Play Console answer guide for the current app build. Re-check it before submission if code, SDKs, permissions, ads, analytics, sharing, or network features are added.

## Current technical state

- Package: `com.coloursprout.kids`
- Version: `1.0.19` / version code `20`
- Target SDK: `35`
- Declared Android permissions: none
- Ads: no
- Analytics: no
- Accounts/login: no
- In-app purchases: no
- Social features/chat/user generated public content: no
- External tracking: no
- App behavior: offline colouring game with local progress saving and local PNG export

## Data safety form guidance

Based on the current build:

- Does the app collect or share user data? `No`
- Is all user data collected by the app encrypted in transit? `Not applicable` because no user data is collected or transmitted.
- Does the app provide a way for users to request data deletion? `Not applicable` for cloud/account data. The app stores colouring progress locally on-device only. Users can clear app storage or uninstall the app to remove local data.
- Is the app committed to following Google Play Families policy? `Yes`, if the selected target audience includes children.

Important: Google Play still requires the Data safety form and a privacy policy link even if the app collects no user data.

## Privacy policy summary

Use `privacy-policy-draft.md` as the basis for a hosted privacy policy page. Update these placeholders before submission:

- Support email
- Company/developer address if needed
- Effective date if you want a different date
- Hosted privacy policy URL

## Families policy checklist

For a child-directed submission, keep this true:

- No advertising SDKs.
- No analytics SDKs.
- No third-party tracking SDKs.
- No collection or transmission of advertising ID, device identifiers, phone number, precise location, contacts, photos, audio, or account data.
- No external links that let children leave the app without an adult gate.
- No social or open communication features.
- No purchases or monetization prompts.
- All artwork must remain original, public domain, CC0, or properly licensed for reuse.

If you add any SDK later, confirm it is approved for child-directed services before release.

## Content rating questionnaire guidance

Expected answers for the current build:

- Violence: none
- Fear/horror: none
- Sexual content/nudity: none
- Language: none
- Controlled substances: none
- Gambling: none
- Online interaction: no
- User-generated public content: no
- Shares user location: no
- Purchases: no
- Ads: no

The likely rating should be suitable for everyone, but the final rating is assigned by Google's questionnaire system.
