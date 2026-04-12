# 1.2. General conventions

## 1.2.1. Documentation
Documentation is structured using [arc42](https://arc42.org/) style.
Documentation should be written in Markdown and enhanced by agreed upon plugins/tools

Supplementary tools:
- [Mermaid graphs within Markdown](#graphs)
- Diagrams.net/Draw.io graphs in editable PNGs

### Graphs
Various graphs like flow charts or sequence diagrams can be written in Mermaid inside Markdown files.
This should be used for most graphs in documentation aside from really complex ones (see Drawing boards).

References:
- [IntelliJ PLugin](https://plugins.jetbrains.com/plugin/20146-mermaid)
- [Mermaid documentation](https://mermaid.ai/open-source/intro/)
- [Mermaid online editor](https://mermaid.live)

### Drawing boards
More complex boards can be drawn using Diagrams.net and exported as editable PNGs.

To start new board you use template file with ready to use standard shapes: [board template file](01_99_design_board_template.png).
While editing it's advised to load the common shapes into scratchpad which can be found here: [scratchpad shapes](01_99_design_board_scratchpad_shapes.xml).

Note that those PNGs can be easily edited within IntelliJ via dedicated plugin.
See references below for link or just search for Diagram.net plugin.

References:
- [IntelliJ PLugin](https://plugins.jetbrains.com/plugin/15635-diagrams-net-integration)
- [Online editor](https://app.diagrams.net/?src=about)