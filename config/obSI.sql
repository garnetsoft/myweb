
{s:first exec sym from x; utime:last x`time; bbook:flip `sym`bids`bsizes`side!(s; raze x`bids; raze x`bsizes;`B); abook:flip `sym`asks`asizes`side!(s; raze x`asks; raze x`asizes;`S); book:(`asks xdesc abook) uj bbook; :(`sym`asizes`asks`bids`bsizes`side xcols update update_time:utime from book) } 0!select by sym from (update bvwap:{y wavg x}'[bids;bsizes], bbs:{sum x} each bsizes, aas:{sum x} each asizes, avwap:{y wavg x}'[asks;asizes] from select from quotelevel2 where sym=`SI, maturity=(first exec maturity from `ct xdesc (select ct:count i by maturity from quotelevel2 where sym=`SI)), excdt.date=.z.D) 


