# Changelog
All notable changes to this project will be documented in this file.

## 1.5.0
### Changed
- Bumped the graylog image to 3.3
- Increased the version of some of the images
- Updated the services to fall in line with how the offical graylog documentation has them
- Set the container names explicitly
- Mounting the /usr/share/graylog/plugin volume explicitly so the jar can be manually built and copied into place

### Added
- Added some logging
- Modified the configuration options.  The 'queue' has been removed in favor of an 'exchange'.  This is because the core model in RMQ is "a producer never sends any messages directly to a queue".
- Added an optional routing_key configuration option.
- Removed the Configuration check.  This is done within the Graylog library itself so I wasn't sure that it was warranted.
- Added a mandatory configuration item of 'vhost'.  This will allow connecting to the non-default vhost.
- Added new configuration options.
- Increased the logging verbosity
- Create a dummy channel so that we can validate an exchange exists.  If it doesn't then we'll use the proper channel to create it, otherwise we just move on.
- Modified the basicPublish to use the routing_key

## 1.4.1
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
