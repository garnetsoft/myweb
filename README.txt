	public String createOrderBookView() {
		StringBuilder tableHtml = new StringBuilder();

		tableHtml
				.append("<style type=text/css> TD{font-family: Arial; font-size: 8pt;} table { border-collapse:collapse; } table, td, th { border:1px solid black; } th { background-color:green; color:white; } td {text-align:right;} </style>");
		tableHtml.append("<br>");

		tableHtml.append("<table id=\"").append(System.nanoTime())
				.append("\"border=\"1\">");
		tableHtml.append("<tr BGCOLOR=\"#99CCFF\">");

		for (String hd : headerNames) {
			tableHtml.append("<td>").append(hd).append("</td>");
		}
		tableHtml.append("</tr>");

		Map<Double, Level> askBook = book.getAskBook();
		Map<Double, Level> bidBook = book.getBidBook();

		if (askBook.size() < 1 || bidBook.size() < 1) {

			tableHtml.append("<tr BGCOLOR=\"ORANGE\"><b>");
			tableHtml.append("<td>").append("EMPTY BOOK").append("</td>");
			tableHtml.append("<td>").append("ASK BOOK: " + askBook.size())
					.append("</td>");
			tableHtml.append("<td>").append("BID BOOK: " + bidBook.size())
					.append("</td>");
			tableHtml.append("</b></tr>");
			tableHtml.append("</table>").append("<br/>");

			log.error("ERROR OrderBook2: bid or ask book emptry");
			log.error(tableHtml.toString());

			return tableHtml.toString();
		}

		int rc = 0; // row color format

		tableHtml.append(processBook(rc, Order.EnSide.sell));

		tableHtml.append("<tr BGCOLOR=\"ORANGE\"><b>");
		tableHtml.append("<td>")
				.append(String.format("%.2f", book.computeSpread()))
				.append("</td>");
		// tableHtml.append("<td>").append(book.getSpreadInfo()).append("</td>");
		tableHtml.append("<td>").append(book.getBookCenterOfMassInfo())
				.append("</td>");
		tableHtml.append("<td>").append(book.getBookOrdersInfo())
				.append("</td>");
		tableHtml.append("<td>").append(book.getBookSizeInfo()).append(" ");
		tableHtml.append(book.getBookNotionalInfo()).append("</td>");
		// tableHtml.append("<td>").append(String.format("s/b (USD): %.2f",
		// bookDelta)).append("</td>");
		tableHtml.append("<td>")//.append(formatIt(book.getSnapshotTime()))
				.append(", lastTrade: ").append(book.getLastTrade())
				.append("</td>");
		tableHtml.append("</b></tr>");

		tableHtml.append(processBook(rc, Order.EnSide.buy));

		tableHtml.append("</table>").append("<br/>");

		return tableHtml.toString();
	}

	// by bid/ask books
	public String processBook(int rc, Order.EnSide side) {
		StringBuilder tableHtml = new StringBuilder();

		Map<Double, Level> tmp = book.getBook(side);
		double vwap = book.getBookCenterOfMass(side);

		List<Double> prices = new ArrayList<Double>(tmp.keySet());
		Collections.sort(prices, Collections.reverseOrder()); // sort by desc

		// find vwap level
		int idx = -1;
		for (int i = 0; i < prices.size() - 2; i++) {
			if (prices.get(i) > vwap && prices.get(i + 1) < vwap)
				idx = i;
		}

		for (int j = 0; j < prices.size(); j++) {
			Level pl = tmp.get(prices.get(j));

			if (pl == null) {
				log.error("Null price Level, px: " + j);
				// new Exception().printStackTrace();

				continue;
			}

			if (idx == j)
				tableHtml.append("<tr BGCOLOR=\"ORANGE\">");
			else {
				if (rc % 2 == 0) {
					tableHtml.append("<tr>");
				} else {
					tableHtml.append("<tr BGCOLOR=\"#99CCFF\">");
				}
			}

			// total size
			tableHtml.append("<td>")
					.append(String.format("%.8f", pl.getSize()))
					.append("</td>");
			// price
			tableHtml.append("<td>").append(pl.getPrice()).append("</td>");

			tableHtml
					.append("<td>")
					//.append(pl.getChildOrderContainer().getChildOrders().size())
					.append("</td>");
			// child order IDs
			StringBuilder sb = new StringBuilder();
//			for (ChildOrder o : pl.getChildOrderContainer().getChildOrders()) {
//				sb.append(o.getSize()).append(", ");
//			}
			tableHtml
					.append("<td>")
					.append(sb.toString().substring(0,
							sb.toString().length() - 2)).append("</td>");

			// orderid
			// tableHtml.append("<td>").append(pl.getChildOrderContainer().getChildOrders().get(0).getOrderID()).append("</td>");
			tableHtml.append("<td>").append(pl.getOrdinalID()).append("</td>");
			tableHtml.append("</tr>");

			rc++;
		}

		return tableHtml.toString();
	}
