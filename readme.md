# Rainbird Sprinkler Driver

This project has no affiliation with Rain Bird. This module works with the Rain Bird LNK WiFi Module. For more information see [http://www.rainbird.com/landscape/products/controllers/LNK-WiFi.htm](http://www.rainbird.com/landscape/products/controllers/LNK-WiFi.htm)

It provides a driver for integrating into the hubitat home automation hub.

## Installation

Here are the steps to install this driver in hubitat

* Logon to your hubitat hub
* Select "Drivers Code" in the left hand nav
* Select the "New Driver" button in the top right corner
* Past in the code from https://raw.githubusercontent.com/craigde/hubitat-rainbird/master/rainbird.groovy
* Save
* Select "Devices" in the left hand nav
* Select the "New Virtual Device" button in the top right corner
* Enter a name in "Device Name"
* Select "Rainbird Sprnkler Controller Driver" from the "Type" drop down
* Select "Save Device"

## Preferences

Now there are a few simple preferences you need to setup. Open the device you created and enter these preferences

* "Where?" - Enter the IP address of your Rainbird WiFi module. If you don't know what this is you can find it in the Rainbird mobile app under "Controller Settings | Network Info"
* "Password" - Enter the password of for your Rainbird WiFi module.
* "How often" - How often to poll in
* "Debug Mode" - Enbale this to generate troubleshooting information in the hubitat log. If the device is working well turn it off so it does not fill your log.

Click "Save Preferences"

If everything works as expected the driver should should refresh with some information displaying current states from the device. They are

* currentDate : MMDDYYYY on your controller
* currentTime : HHMMSS on your controller
* modelID : Model ID as a number
* protocolRevisionMajor :
* protocolRevisionMinor :
* rainDelay : If a rain delay is set the number of days. Otherwise 0.
* serialNo : Serial Number of your controller
* watering : True if watering currently running otherwise false

## Usage

I orginally started this project because I wanted to be able to set the rain delay based on precipitation. I implemntated enough functionality to enable this scenario. I use this driver and a rule so that if my weather driver sees suffcient rain it will set the rain delay to 1 day skipping watering on wet days.  If others are interested I can finish the driver and add the other missing commands that would support more granular sprinkler and zone controls.
