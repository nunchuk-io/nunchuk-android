## [1.9.20]

- [NUN-2071](https://nunchuck.atlassian.net/browse/NUN-2071)
- Replace logo
- [NUN-2066](https://nunchuck.atlassian.net/browse/NUN-2066)
- Ask user to tap TAPSIGNER again if GetDefaultSignerFromMasfer failed
- [NUN-2057](https://nunchuck.atlassian.net/browse/NUN-2057)
- Support user change bip32 path when create wallet
- [NUN-2059](https://nunchuck.atlassian.net/browse/NUN-2059)
- Health check coldcard
- [NUN-1918](https://nunchuck.atlassian.net/browse/NUN-1918)
- Primary Key

- [NUN-2037](https://nunchuck.atlassian.net/browse/NUN-2037)
- Fix wallet name not update
- [NUN-2029](https://nunchuck.atlassian.net/browse/NUN-2029)
- Fix show btc unit
- [NUN-2025](https://nunchuck.atlassian.net/browse/NUN-2025)
- Refactor sync room
- [NUN-1953](https://nunchuck.atlassian.net/browse/NUN-1953)
- Update qr generate, logic check signer in wallet
- [NUN-2026](https://nunchuck.atlassian.net/browse/NUN-2026)
- Fix overlap when select message in room
- [NUN-1970](https://nunchuck.atlassian.net/browse/NUN-1970)
- Fix Member keys list key that doesn't belong in wallet
- [NUN-1969](https://nunchuck.atlassian.net/browse/NUN-1969)
Support auto reload transaction in transaction detail
- [NUN-1993](https://nunchuck.atlassian.net/browse/NUN-1993)
- Add a "Remove transaction" option to the menu for Network Rejected transactions
- [NUN-2024](https://nunchuck.atlassian.net/browse/NUN-2024)
- Remove all warning airgap has been used
- [NUN-1928](https://nunchuck.atlassian.net/browse/NUN-1928)
- Support select address type

## [1.9.19]

### Feature

- [NUN-1192](https://nunchuck.atlassian.net/browse/NUN-1192)
- Minimum number of Total Keys in a collab wallet should be max(2, number of members in room)
- [NUN-1882](https://nunchuck.atlassian.net/browse/NUN-1882)
- Call NunchukMatrixImpl::SignAirgapTransaction for MK4 NFC
- [NUN-1869](https://nunchuck.atlassian.net/browse/NUN-1869)
- Updated send flow
- [NUN-1764](https://nunchuck.atlassian.net/browse/NUN-1764)
- Sign airgap transaction
- Implement integration with Mk4
- [NUN-1866](https://nunchuck.atlassian.net/browse/NUN-1866)
- Make address and tx id copyable
- [NUN-1854](https://nunchuck.atlassian.net/browse/NUN-1854)
- NFC new icon
- [NUN-398](https://nunchuck.atlassian.net/browse/NUN-398)
- Wallet config styling fix + functionality
- [NUN-1774](https://nunchuck.atlassian.net/browse/NUN-1774)
- Replace by fee new UI

### Fixed

- Rewrite matrix flow
- [NUN-70](https://nunchuck.atlassian.net/browse/NUN-70)
- Only accept (auto-join) room requests from our Contacts
- [NUN-661](https://nunchuck.atlassian.net/browse/NUN-661)
- Transaction canceled in chat is not removed in wallet
- [NUN-1408](https://nunchuck.atlassian.net/browse/NUN-1408)
- Large psbt: can not create transaction
- [NUN-803](https://nunchuck.atlassian.net/browse/NUN-803)
- Wallet empty state missing copied address and share address button
- [NUN-1600](https://nunchuck.atlassian.net/browse/NUN-1600)
- Contacts tab: Request does not count received request

## [1.9.18]

### Feature

- [NUN-1729](https://nunchuck.atlassian.net/browse/NUN-1729)
  Integrate Satscard to Nunchuk app
- [NUN-1767](https://nunchuck.atlassian.net/browse/NUN-1767)
  Add ability for user to sweep SATSCARD to external address
- [NUN-1737](https://nunchuck.atlassian.net/browse/NUN-1737)
  Add Show unsealed slots option to Setup SATSCARD modal
- [NUN-1741](https://nunchuck.atlassian.net/browse/NUN-1741) Update Blockstream to mempool
- [NUN-1705](https://nunchuck.atlassian.net/browse/NUN-1705) Update Read out guide
- [NUN-1721](https://nunchuck.atlassian.net/browse/NUN-1721) Get fee rate from API
- [NUN-231](https://nunchuck.atlassian.net/browse/NUN-231) Support off-chain note
- [NUN-1673](https://nunchuck.atlassian.net/browse/NUN-1673) Allow user to delete collab wallet
- Migrate new Matrix library version

### Fixed

- [NUN-1276](https://nunchuck.atlassian.net/browse/NUN-1276) Display message when change password success
- [NUN-1673](https://nunchuck.atlassian.net/browse/NUN-1673) When create collab wallet, recipient can finalize wallet
- [NUN-1782](https://nunchuck.atlassian.net/browse/NUN-1782) Logged in devices: text is overlap
- Cache BTC price