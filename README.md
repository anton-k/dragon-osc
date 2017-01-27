OSC-controller
===============================================

The dragon-osc is an app for building custom OSC-controllers.
We can specify the UI with markup language that is based on YAML or JSON file.
We specify the layout of the widgets and the interaction of the parts.

The primary goal of the app is to receive and send OSC-messages. 

Features:

* Many widgets useful for audio: toggles, knobs, faders, buttons, XY-pads, etc

* Easy way to make complex layouts with relative sizes.

* We can send OSC-messages to many OSC-clients. 

* We can send many OSC-messages at the same time (based on the single event). It can be
  useful for setting up a scene for a track. 

* Dynamic update of the widgets look. The app itself can be changed with specific OSC messages

* We can trigger and control all widgets with code by sending OSC-messages, 
  so the app can be used as a visual guide for generative music. Imagine an algorithm
  that sends the control messages to the app and the app sends it to synthesizers.

* We can create complex keyboard hot-keys and hot-keys can be specific to the selected window/tab-page.
   This can turn our keyboard to a musical controller.

* The app-config file is JSON or YAML file it makes it easy to create layouts with code.


### Installation

To run the app we need git and sbt. Git is for cloning the repo and sbt 
is for building everything. The app is made with Scala. The sbt is a standard tool to make Scala applications and libraries.

Clone the repo:

~~~
> git clone https://github.com/anton-k/dragon-osc.git
~~~

Navigate to the directory  and run `sbt`:

~~~
> cd dragon-osc
> sbt
~~~

Now we can run the app with run command:

~~~
> run -i /path/to/config.yaml
~~~

Where `config.yaml` is our own file where we can specify our OSC-controller.


### Hello world!

### Let's add some toggles and knobs

### Let's make tabs

### The OSC-messages

### Hot-keys

### Widgets

### Colors




