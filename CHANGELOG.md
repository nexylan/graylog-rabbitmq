# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]
### Fixed
- Not working json format message.

### Security
- Update dependencies's versions to fix: CVE-2018-12022, CVE-2017-17485, CVE-2018-7489, CVE-2017-7525, CVE-2018-11087

## 1.4.0
### Added
- Option to define the format of the RabbitMQ message, JSON of all fields or Plain message

## 1.3.0
### Added
- Durable option for the queue in output configuration

## 1.2.1
### Changed
- Change the way to define the ttl.
- Now we use custom parameters when sending the message

## 1.2.0
### Added
- TTL management by editing the queue definition

## 1.1.0
### Fixed
- `FullMessage` not supported on gentoo

## 1.1.0
### Added
- Authentication for RabbitMQ
