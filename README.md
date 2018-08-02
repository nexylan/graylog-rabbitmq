# RabbitMq Plugin for Graylog


__This plugin is an output plugin which permit to Graylog, to send logs to a RabbitMQ Server.__

**Required Graylog version:** 2.0 and later

Installation
------------

[Download the plugin](https://github.com/https://git.nexylan.net/hdevigne/graylog-rabbitmq/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

Usage
-----

__To Write....__


Getting started
---------------

This project is using Maven 3 and requires Java 8 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart the Graylog.

Plugin Release
--------------
We use Gitlab in intern and perform CI to verify the quality of our plugin.


Contributing
--------------
If you want contribute to this project, you can submit issue on the Gist. 