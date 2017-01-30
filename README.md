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

Also by typing in sbt prompt:

~~~
> assembly
~~~

We can create a single jar which we can run with java:

~~~
> java -jar dragon-osc.jar
~~~

The jar-file will be written in the directory target/scala-2.*

### Hello world!

Let's create a simple UI program and invoke it with our program. 

~~~yaml
main:
   - window:
      title: app
      size: [200, 100]
      content:
        label:
          text: "Hello World!"
~~~

Let's save the file to hello.yaml load the project with `sbt` and invoke the app with it:

~~~
> sbt
> run -i hello.yaml
~~~

The flag `-i` says where is the config file for the app.
Every config-file should has the root element called `main`. 
Then within the `main` we list the windows of the app. 

Window has the attributes: `title` (string), `size` (pair encoded as list), `content` (all ui elements).
We have defined a single UI-element that only shows the string. The widget is a `label`.

#### Simple layout 

Let's go to the more interesting widgets. We can stack many widgets horizontally or vertically with `hor` and `ver`.
Let's create some knobs:

~~~yaml
main:
   - window:
      title: app
      size: [200, 100]
      content:
        hor:
          - dial: 
              init: 0.25
              color: "orange"
          - dial:
              init: 0.5
              color: "blue"              
          - dial:
              init: 0.75
              color: "olive"
~~~

We create three knobs with different initial values and colors.
We can change the values with mouse. Try to change the `hor` with `ver` and see what happens.

How can we send the OSC-messages with those knobs?

#### The OSC-messages

The OSC-message consists of three parts:

* `client` to whom we send the message

* `path`  on which the recipient listens

* `args` the list of arguments


Here is the simple message:

~~~yaml
msg:
    client: jack
    path:   /amp
    args:   [0.2]
~~~

We can specify the message with attribute `send`:

~~~yaml
- dial: 
    init: 0.25
    color: "orange"
  send:
    - msg:
        client: jack
        path:   /amp
        args:   [$0]
~~~

Notice that the field `send` is on the same level as dial` (not inside the send's fields).

The special syntax `$int` means read the value from the N's input of the current value of the widget.
In this example we read a single value of the knob. 

The OSC-servers listen on specific ports. In the app the ports are specified by names. We assign the
actual port to the name in the command line argument `-c`  or `--clients`: 

~~~
run --verbose  -i hello.yaml -c jack=7654
~~~

Also we specify the flag `--verbose` to see the prints of the sent messages. 

### Faders

We can substitute dials with faders:

~~~yaml
main:
   - window:
      title: app
      size: [300, 100]
      content:
        ver:
          - hor:
              - label:
                  text: cps
              - hfader: 
                  init: 0.25
                  color: "orange"
                send:
                  - msg:
                      client: synth
                      path:   /amp
                      args:   [$0]
          - hor:
              - label:
                  text: amp
              - hfader:
                  init: 0.5
                  color: "blue" 
                send:
                  - msg:
                      client: synth
                      path:   /amp
                      args:   [$0]
~~~

We control two parameters amplitude and frequency of our synthesizer. 

### Buttons and toggles

Let's send the notes in the separate window. The content of the first window is going to be the same but
we are going to add another window with button and toggle:

~~~yaml
main:
   - window:
     title: ap1
     content:
        ...
        ...
   - window:
      title: ap2
      content:
        hor:
          - button:
              color: orange
            send:
              - msg: { client: "synth", path: "/play-note", args: [440] }
          - toggle:
              init: false
              color: green
            send: 
              - msg: { client: "synth", path: "/mute", args: [$0] }
~~~

The button sends the message when it's pressed.
The toggle can send two types of messages. Also we can specify
the toggle messages with special syntax:

~~~yaml
          - toggle:
              init: false
              color: green
            send: 
              case true:
                  - msg: { client: "synth", path: "/play", args: [] }
              case false:
                  - msg: { client: "synth", path: "/stop", args: [] }
~~~

We can write `case value` and the message is going to be sent only if current value
equals to the case. Also we can use this method with integer and string

There are many more widgets you can check out the full list at the section `Widgets` of this guide.

### Let's make tabs

Also we can group UI's with tabs:

~~~yaml
tabs:
    - page:
        title: first
        content: ...
    - page:
        title: second
        content: ...
~~~

### Using app as a server

We can not only send OSc-messages also we can receive messages.
We can send the messages to app to simulate the user interaction
and to change some basic look and feel attributes (like colors or texts).

To receive messages we should specify the port on which we expect messages to come.
we do it with command line flag `-s` or `--server`:

~~~
> run -s 8800 -i config.yaml -c unit=7000
~~~

To send the message to interact with the widget we need to give it a name or an `id`entifier:
Let's for example give the identifier to the fader:

~~~
vfader:
    init: 0.2
    color: olive
id: amp
~~~

We can send the OSC-message to our app to update the value of `amp` fader:

~~~yaml
{ path: "/amp", args: [0.5] }
~~~

This will update the visual widget and send all the messages for the widget. 
There is a hack if we want only to update the visual representation but not send the 
messages of the widget. We should prefix with "/cold":

~~~yaml
{ path: "/cold/amp", args: [0.5] }
~~~

Like this we can set the toggles  or click the button (with button the argument list is empty).
The general idea is to write the name of the identifier in the path and set the value in the arguments.

#### Updating the float producing widgets

We can make relative changes. We can add specific amount to the current value of the float producing widget (like `dial`, `hfader` or `vfader`).

~~~
yaml
{ path: "/id/add-float", args: [0.1] }
~~~

We write id in the path next we write `add-float` and pass the value to add in the argument.

#### Updating values of the toggles

We can switch the value of the toggle with syntax:

~~~
{ path: "/id/toggle", args: [] }
~~~

There is a useful widget called `multi-toggle` it contains a matrix of toggles.
We can toggle the value in the give cell like this:

~~~
{ path: "/id/multi-toggle", args: [2] }
~~~

#### Set colors

We can set colors:

~~~
{ path: "/id/set-color", args: [color-name] }
~~~

#### Set texts

We can set text for labels:

~~~
{ path: "/id/set-text", args: [text] }
~~~

### Hot-keys

### Widgets


### Colors

Here is the list of color names: 

~~~
navy, blue, aqua, teal, olive, green, lime, yellow, 
orange, red, maroon, fuchsia, purple, black, gray, silver, white, any.
~~~

`any` -- means any color at random. 




