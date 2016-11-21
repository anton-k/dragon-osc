import swing.event.SelectionChanged

private val tp = new TabbedPane() {
  pages += new TabbedPane.Page("Deck0",new ScrollPane(tables(0)))
  pages += new TabbedPane.Page("Deck1",new ScrollPane(tables(1)))
}

reactions += {
  case SelectionChanged( x ) => println( "changed to %d" format(tp.selection.index))
  case e => println("%s => %s" format(e.getClass.getSimpleName, e.toString))
}

listenTo( tp.selection )  // this is required