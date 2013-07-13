package ar.com.hjg.pngj;

import java.io.InputStream;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the DeflatedChunkSet using a ChunkSetReader2 that reads the IDATs with
 * no knowledge of filters, etc
 */
public class DeflatedChunkSetTest {

	public static class ChunkSetReaderIdatRawCb extends ChunkSeqReader { // callback
		final int rowsize;
		private int nrows;
		private int rown;
		private StringBuilder summary = new StringBuilder(); // debug/test

		public ChunkSetReaderIdatRawCb(int rowsize, int nrows) {
			super();
			this.rowsize = rowsize;
			this.nrows = nrows;
			rown = 0;
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			DeflatedChunksSet cc = new DeflatedChunksSet(id, rowsize, rowsize) {
				@Override
				protected int processRowCallback() {
					summary.append(TestSupport.showRow(getInflatedRow(), getRowFilled(), rown)).append(" ");
					rown++;
					return rown >= nrows ? -1 : rowsize;
				}
			};
			cc.setCallbackMode(true);
			return cc;
		}

		@Override
		protected boolean isIdatKind(String id) {
			return id.equals("IDAT");
		}
	}

	public static class ChunkSetReaderIdatRaw extends ChunkSeqReader {// normal (polled,sync) callback
		final int rowsize;
		private int nrows;
		private int rown;
		private StringBuilder summary = new StringBuilder(); // for debug/tests

		public ChunkSetReaderIdatRaw(int rowsize, int nrows) {
			super();
			this.rowsize = rowsize;
			this.nrows = nrows;
			rown = 0;
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			DeflatedChunksSet cc = new DeflatedChunksSet(id, rowsize, rowsize) {
			};
			cc.setCallbackMode(false);
			return cc;
		}

		@Override
		protected boolean isIdatKind(String id) {
			return id.equals("IDAT");
		}

		public void readFrom(InputStream is) {
			BufferedStreamFeeder bf = new BufferedStreamFeeder(is, TestSupport.randBufSize());
			while (curReaderDeflatedSet == null) {
				if (bf.feed(this) < 1)
					break;
			}
			for (rown = 0; rown < nrows; rown++) {
				while (!isDone() && curReaderDeflatedSet != null && ! curReaderDeflatedSet.isRowReady()) {
					if (bf.feed(this) < 1)
						break;
				}
				if (curReaderDeflatedSet == null)
					break;
				summary.append(
						TestSupport.showRow(curReaderDeflatedSet.getInflatedRow(), curReaderDeflatedSet.getRowFilled(),
								curReaderDeflatedSet.getRown())).append(" ");
				curReaderDeflatedSet.prepareForNextRow(rowsize);
			}
			if(curReaderDeflatedSet!=null)
				curReaderDeflatedSet.end();

			while (!isDone()) {
				if (bf.feed(this) < 1)
					break;
			}
			bf.end();
		}
	}

	@Test
	public void read1CbExact() {
		ChunkSetReaderIdatRawCb c = new ChunkSetReaderIdatRawCb(4, 3); // "true" values
		TestSupport.feedFromStreamTest(c, "resources/test/testg2.png");
		TestCase.assertEquals(181, c.getBytesCount());
		// warning: this is unfiltered 
		TestCase.assertEquals("r=0[  1|  0   1   1] r=1[  3|112 136   8] r=2[  1|255 239 238] ", c.summary.toString());
	}

	@Test
	public void read1CbLessBytes() {
		ChunkSetReaderIdatRawCb c = new ChunkSetReaderIdatRawCb(3, 2);
		TestSupport.feedFromStreamTest(c, "resources/test/testg2.png");
		TestCase.assertEquals(181, c.getBytesCount());
		TestCase.assertEquals("r=0[  1|  0   1] r=1[  1|  3 112] ", c.summary.toString());
	}

	@Test
	public void read1CbMoreBytes() {
		ChunkSetReaderIdatRawCb c = new ChunkSetReaderIdatRawCb(5, 9);
		TestSupport.feedFromStreamTest(c, "resources/test/testg2.png");
		TestCase.assertEquals(181, c.getBytesCount());
		TestCase.assertEquals(6, c.getChunkCount());
		TestCase.assertEquals("r=0[  1|  0   1   1   3] r=1[112|136   8   1 255] r=2[239|238] ",
				c.summary.toString());
	}

	@Test
	public void read1PollExact() {
		ChunkSetReaderIdatRaw c = new ChunkSetReaderIdatRaw(4, 3); // "true" values
		c.readFrom(TestSupport.istream("resources/test/testg2.png"));
		TestCase.assertEquals(181, c.getBytesCount());
		//System.out.println(c.summary);
		TestCase.assertEquals("r=0[  1|  0   1   1] r=1[  3|112 136   8] r=2[  1|255 239 238] ", c.summary.toString());
	}

	@Test
	public void read1PollLessBytes() {
		ChunkSetReaderIdatRaw c = new ChunkSetReaderIdatRaw(3, 2);
		c.readFrom(TestSupport.istream("resources/test/testg2.png"));
		TestCase.assertEquals(181, c.getBytesCount());
		//System.out.println(c.summary);
		TestCase.assertEquals("r=0[  1|  0   1] r=1[  1|  3 112] ", c.summary.toString());
	}

	@Test
	public void read1PollMoreBytes() {
		ChunkSetReaderIdatRaw c = new ChunkSetReaderIdatRaw(5, 9);
		c.readFrom(TestSupport.istream("resources/test/testg2.png"));
		TestCase.assertEquals(181, c.getBytesCount());
		//System.out.println(c.summary);
		TestCase.assertEquals(
				"r=0[  1|  0   1   1   3] r=1[112|136   8   1 255] r=2[239|238] ",
				c.summary.toString());
	}
	
	@Test(expected=PngjInputException.class)
	public void read1PollBad() { // file has missing IDAT
		ChunkSetReaderIdatRaw c = new ChunkSetReaderIdatRaw(81, 300); // "true" values
		c.readFrom(TestSupport.istream("resources/test/bad_missingidat.png"));
	}


	@Before
	public void setUp() {

	}

	/**
	 * Tears down the test fixture. (Called after every test case method.)
	 */
	@After
	public void tearDown() {
		TestSupport.cleanAll();
	}

}
