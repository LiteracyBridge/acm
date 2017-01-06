# Setting up ADB over wi-fi
 
1. Be sure both the device and computer are on the same wifi network.
1. Connect device to computer via USB cable.
1. Start ADB in USB mode, and be sure device is attached:
    ```bash
     $ adb usb
     * daemon not running. starting it now on port 5037 *
     * daemon started successfully *
     restarting in USB mode
     ```
1. Get the IP address of the device:
    ```bash
    $ adb shell netcfg
    wlan0    UP                                192.168.1.22/24  0x00001043 9c:5c:8e:66:b1:14
    sit0     DOWN                                   0.0.0.0/0   0x00000080 00:00:00:00:00:00
    lo       UP                                   127.0.0.1/8   0x00000049 00:00:00:00:00:00
    ```
1. Restart ADB in TCP mode:
    ```bash
    $ adb tcpip 5556
    restarting in TCP mode port: 5556
    ```
1. Connect to the device via TCP:
    ```bash
    $ adb connect 192.168.1.22:5556
    connected to 192.168.1.22:5556
    $ adb devices
    List of devices attached
    192.168.1.22:5556	device
    G3NPFP111096BND	device
    ```