# yetibot.core

[![Build Status](https://img.shields.io/travis/com/yetibot/yetibot.core?style=for-the-badge)](https://travis-ci.com/yetibot/yetibot.core)
[![Clojars](https://img.shields.io/clojars/v/yetibot/core?style=for-the-badge)](https://clojars.org/yetibot/core)
[![Codecov](https://img.shields.io/codecov/c/github/yetibot/yetibot.core?style=for-the-badge)](https://codecov.io/gh/yetibot/yetibot.core)

Core Yetibot utilities, extracted for shared use among Yetibot and its various
plugins. yetibot.core is not meant to be run standalone, but instead used as a
dependency from another project that provides config and optionally other
Yetibot plugins, private or public.

The majority of Yetibot commands live in the [main Yetibot
repo](https://github.com/yetibot/yetibot).

- [CHANGELOG](doc/CHANGELOG.md)
- [yetibot.com](https://yetibot.com)

## Usage

You can depend on this library to build your own Yetibot plugins.
Building your own commands is dead simple. Here's an example command that
adds two numbers:

```clojure
(ns mycompany.plugins.commands.add
  (:require [yetibot.core.hooks :refer [cmd-hook]]))

(defn add-cmd
  "add <number1> <number2> # Add two numbers"
  [{[_ n1 n2] :match}] (+ (read-string n1) (read-string n2)))

(cmd-hook #"add" ; command prefix
          #"(\d+)\s+(\d+)" add-cmd)
```

See Yetibot's own [commands](https://github.com/devth/yetibot/tree/master/src/yetibot/commands)
for more complex and diverse examples.

## Remote REPL

Yetibot runs an embedded nREPL server on port `65432`. Connect to it via:

```
nrepl://localhost:65432
```

Or replace `localhost` with the remote network address.

## yetibot-dashboard

`yetibot-dashboard` is an NPM module that contains static HTML/JS/CSS for the
Yetibot dashboard. It's used by yetibot.core via
[`lein-npm`](https://github.com/RyanMcG/lein-npm) and served by
yetibot.core's Ring server. A public example can be seen at
[public.yetibot.com](https://public.yetibot.com).

To update to a newer version:

1. Bump the `yetibot-dashboard` dep in `project.clj`
1. Run `lein deps`
1. Commit the updated `package-lock.json`

## Docs

- [User Guide](https://yetibot.com/user-guide/)
- [Dev Guide](https://yetibot.com/dev-guide/)
- [Ops Guide](https://yetibot.com/ops-guide/)

## Change Log

View the [CHANGELOG](doc/CHANGELOG.md).

## License

Copyright © 2013–2019 Trevor C. Hartman

Distributed under the Eclipse Public License version 1.0.
