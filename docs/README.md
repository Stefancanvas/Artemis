# Artemis Documentation

We use [Sphinx] for creating the Artemis documentation.
Documentation is written in [reStructuredText] (RST).
To get started with RST, check out the [Quickstart] or this [cheatsheet].

## Documentation Hosting

The [Artemis documentation] is hosted on [Read the Docs] (RtD) for the `develop` (latest) branch, as well as for git tags. The latest tag is always linked as the as _stable_ version.
RtD will build and deploy changes automatically.

## Installing Sphinx Locally
[Sphinx] can run locally to generate the documentation in HTML and other formats.
You can install Sphinx using `pip` or choose a system-wide installation instead.
When using pip, consider using [Python virtual environments].
```bash
pip install -r requirements.txt
```
More install options are explained in the [Installing Sphinx] documentation.

## Running Sphinx Locally

To  generate the documentation as single HTML file, use the provided `Makefile`/`make.bat` files:
```bash
# maxOS / Linux
make singlehtml

# Windows
make.bat singlehtml
```


Using [sphinx-autobuild], the browser will live-reload on changes, ideal for viewing changes while writing documentation:
```bash
# maxOS / Linux
make livehtml

# Windows
make.bat livehtml
```

## Tool support
A list of useful tools to write documentation:
- [reStructuredText for Visual Studio Code](https://www.restructuredtext.net)
- [LanguageTool for Visual Studio Code](https://marketplace.visualstudio.com/items?itemName=adamvoss.vscode-languagetool): Provides offline grammar checking
- [ReStructuredText for IntelliJ](https://plugins.jetbrains.com/plugin/7124-restructuredtext)



<!-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -->
[Artemis documentation]: https://artemis-platform.readthedocs.io

[reStructuredText]: https://docutils.sourceforge.io/rst.html
[Quickstart]: https://docutils.sourceforge.io/docs/user/rst/quickstart.html
[cheatsheet]: http://github.com/ralsina/rst-cheatsheet/raw/master/rst-cheatsheet.pdf

[Sphinx]: https://www.sphinx-doc.org/en/master/
[Installing Sphinx]: https://www.sphinx-doc.org/en/master/usage/installation.html
[Python virtual environments]: https://docs.python.org/3/library/venv.html
[sphinx-autobuild]: https://pypi.org/project/sphinx-autobuild/
[Read the Docs]: https://readthedocs.org
