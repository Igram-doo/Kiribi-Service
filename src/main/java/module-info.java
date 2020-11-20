module rs.igram.kiribi.service {
	requires java.base;
	requires transitive rs.igram.kiribi.crypto;
	requires transitive rs.igram.kiribi.io;
	requires transitive rs.igram.kiribi.net;
	exports rs.igram.kiribi.service;
}