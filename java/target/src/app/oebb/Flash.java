package oebb;

/**
*	Flash.java: Flash handling for OEBB project.
*
*	Author: Martin Schoeberl (martin.schoeberl@chello.at)
*
*   Changelog:
*
*/

import util.*;

import com.jopdesign.sys.Native;

public class Flash {

	public static final int DATA_START = 0xa0000; 	// user data, Streckendaten in oebb BG263
	public static final int BGID_START = 0xb0000; 	// user data, bgid
	public static final int CONFIG_LEN = 256;		// first 256 Bytes are confing data
													// than logbook
	public static final int BG_MASTER = 10*4;
	public static final int BG_MASTER_MAGIC = 1234;



	static final int OFF_VER = 4;
	static final int OFF_LEN = 8;
	static final int OFF_CNT = 12;
	static final int OFF_FIRST = 16;

	static final int STR_NR = 0;
	static final int STR_NET = 4;
	static final int STR_DIAL = 8;
	static final int STR_UID = 12;
	static final int STR_PWD = 16;
	static final int STR_IP = 20;
	static final int STR_CNT = 24;
	static final int STR_LEN = 28;				// first point

	static final int PT_MELNR = 0;
	static final int PT_LAT = 4;
	static final int PT_LON = 8;
	static final int PT_PTR = 12;
	static final int PT_FLAGS = 16;
	static final int PT_LEN = 20;

	// Ankunft flag = station (used in ES)!
	static final int PT_FLG_STATION = 1;
	static final int PT_FLG_VERL = 2;
	static final int PT_FLG_ES = 4;

	static class Point {
		int melnr;
		int lat;
		int lon;
		int ptr;
		int flags;
		boolean ankunft;
		boolean verlassen;
		StringBuffer stationLine1;
		StringBuffer stationLine2;
		StringBuffer verschubVon;
		StringBuffer verschubBis;

		Point() {
			stationLine1 = new StringBuffer(19);
			stationLine2 = new StringBuffer(19);
			verschubVon = new StringBuffer(19);
			verschubBis = new StringBuffer(19);
		}

		// TODO own class for Strecke and Points (immutable with Strings...)
		Point getPrev() {
			int i = Flash.getPrev(melnr);
			if (i==-1) return null;
			return Flash.getPoint(i);
		}
		
		Point getNext() {
			int i = Flash.getNext(melnr);
			if (i==-1) return null;
			return Flash.getPoint(i);
		}
		
	}

	static final int MAX_POINTS = 100;
	/** List of GPS points */
	static Point[] str;
	/** start of Points in Flash */
	static int addrPoints;
	/** Names valid */
	static boolean textOk;
	/** Direction for names */
	static boolean left2right;
	/** Streckennummer */
	static int nrStr;
	/** len of Strecke (in points) */
	static int lenStr;
	/** destination IP address */
	static int dstIp;
	/** connection strings */
	static StringBuffer[] connStr;
	
	static StringBuffer[] tmpStr;
	
	static int logPtr;

	public static void init() {

		nrStr = -1;
		if (str!=null) return;		// allready called

		str = new Point[MAX_POINTS];
		for (int i=0; i<MAX_POINTS; ++i) {
			str[i] = new Point();
		}
		dstIp = 0;
		connStr = new StringBuffer[4];
		connStr[0] = new StringBuffer(20);
		connStr[1] = new StringBuffer(40);
		connStr[2] = new StringBuffer(20);
		connStr[3] = new StringBuffer(20);
		
		tmpStr = new StringBuffer[6];
		for (int i=0; i<6; ++i) {
			tmpStr[i] = new StringBuffer(19);
		}
		textOk = false;
		logPtr = 0;
		
	}

	public static int intVal(int addr) {

		int val = 0;
		for (int i=0; i<4; ++i) {
			val <<= 8;
			val += Native.rdMem(DATA_START+addr+i);
		}

		return val;
	}
	
	public static void forceReload() {
		nrStr = -1;
	}

	/**
	 * How many points in the loaded Strecke?
	 * @return
	 */
	/*
	public static int getStrLen() {
		return lenStr;
	}
	*/
	/**
	*	Find first point to Strecke strnr.
	*/
	public static int getFirst(int strnr) {

		if (nrStr!=strnr) {
			if (!loadStr(strnr)) {
				return -1;
			}
			if (Status.esMode) {
				if (!esStr()) {
					return -1;
				}
			}
		}
		return str[0].melnr;
	}

	/**
	*	Find last point to Strecke strnr.
	*/
	public static int getLast(int strnr) {

		if (nrStr!=strnr) {
			if (!loadStr(strnr)) {
				return -1;
			}
			if (Status.esMode) {
				if (!esStr()) {
					return -1;
				}
			}
		}
		return str[lenStr-1].melnr;
	}

	public static Point getPoint(int melnr) {

		for (int i=0; i<lenStr; ++i) {
			if (str[i].melnr == melnr) {
				return str[i];
			}
		}
		Dbg.wr("\nPoint PROBLEM\n");
		return null;
	}

	/**
	*	Find next 'real' point after melnr.
	*	-1 on last point.
	*/
	public static int getNext(int melnr) {

		for (int i=0; i<lenStr; ++i) {
			if (str[i].melnr == melnr) {
				for(; i<lenStr; ++i) {
					if (str[i].melnr != melnr) {
						return str[i].melnr;
					}
				}
			}
		}
		return -1;
	}

	/**
	*	Find previous point.
	*	-1 on first point.
	*/
	public static int getPrev(int melnr) {

		for (int i=lenStr-1; i>=0; --i) {
			if (str[i].melnr == melnr) {
				for(; i>=0; --i) {
					if (str[i].melnr != melnr) {
						return str[i].melnr;
					}
				}
			}
		}
		return -1;
	}

	static void loadString(StringBuffer str, int addr) {

		int i;

		str.setLength(0);
		addr = intVal(addr);

// Dbg.wr("loadString\n");
// Dbg.intVal(addr);
		if (addr <= 0) {
// Dbg.wr("no String\n");
			return;
		}

		for (i=0; i<80; ++i) {
			int val = Native.rdMem(DATA_START+addr+i);
			if (val==0) break;
// Dbg.wr(val);
			str.append((char) val);
		}
	}


	/**
	*	Load point data from flash to str array.
	*/
	static boolean loadStr(int strnr) {

		if (str==null) init();
		
		textOk = false;
		int i, j, k;
		
		// number of Strecken
		int cnt = intVal(OFF_CNT);
		int addr = OFF_FIRST;
		Point p;
		
		for (i=0; i<cnt; ++i) {
			if (intVal(addr+STR_NR)==strnr) {
				int nrPt = intVal(addr+STR_CNT);
				if (nrPt>MAX_POINTS) {
					Dbg.wr("MAX_POINTS\n");
					return false;
				}
				dstIp = intVal(addr+STR_IP);
/*
possible stack overfolw!!!
				loadString(connStr[0], addr+STR_DIAL);
				loadString(connStr[1], addr+STR_NET);
				loadString(connStr[2], addr+STR_UID);
				loadString(connStr[3], addr+STR_PWD);
*/

				nrStr = strnr;
				lenStr = nrPt;
				addr += STR_LEN;
				addrPoints = addr;
				for (j=0; j<nrPt; ++j) {
					p = str[j];
					p.melnr = intVal(addr+PT_MELNR);
					p.lat = intVal(addr+PT_LAT);
					p.lon = intVal(addr+PT_LON);
					p.ptr = intVal(addr+PT_PTR);
					p.flags = intVal(addr+PT_FLAGS);
					p.ankunft = (p.flags & PT_FLG_STATION)!=0;
					p.verlassen = (p.flags & PT_FLG_VERL)!=0;

					
					addr += PT_LEN;
				}
				return true;
			}
			addr += STR_LEN+intVal(addr+STR_CNT)*PT_LEN;		// find next Strecke
		}
		Dbg.wr("Strecke not found ", strnr);
		return false;											// not found
	}

	/**
	*	Load strings from flash to str array.
	*	Points MUST be loaded with loadStr().
	*/
	static void loadStrNames(int strnr, int von, int bis) {

		boolean l2r = bis-von>=0;
		
		if (l2r==left2right && textOk) return;
Dbg.wr("loadStrNames ");
Dbg.intVal(strnr);
Dbg.lf();
		
		// if we go from left to right,
		// ziel is right name
		int line = l2r ? 2 : 0;
		
		int addr = addrPoints;
		for (int i=0; i<lenStr; ++i) {
			Point p = str[i];
			p.ptr = intVal(addr+PT_PTR);

			int idx = p.ptr;
			if (idx<=0) continue;
			
			int val = ' ';
			int off = 0;
			for (int j=0; j<6 && val!=0; ++j) {
				tmpStr[j].setLength(0);
				for (int k=0; k<19; ++k) {
					val = Native.rdMem(DATA_START+idx+off);
					++off;
					if (val==0 || val=='^') break;
					tmpStr[j].append((char) val);
				}
Dbg.wr(tmpStr[j]);
			}
			p.stationLine1.setLength(0);
			p.stationLine2.setLength(0);
			p.stationLine1.append(tmpStr[line]);
			p.stationLine2.append(tmpStr[line+1]);
			
			p.verschubVon.setLength(0);
			p.verschubVon.append(tmpStr[4]);
			p.verschubBis.setLength(0);
			p.verschubBis.append(tmpStr[5]);
			addr += PT_LEN;
			
		}
		textOk = true;
		left2right = l2r;
	}

	/**
	 * Change the data for ES mode. MUST be called after loadStr()
	 * in ES mode!
	 *
	 */
	static boolean esStr() {
		
System.out.println("ES Strecke:");
		int i, j, k;
		int txtPtr = -1;
		boolean left = true;
		Point p1, p2;
		// Suche die pointer zu den Stationstexten fuer die ES Strecke
		for (i=0; i<lenStr; ++i) {
			p1 = str[i];
			if ((p1.flags&PT_FLG_ES)!=0) {
				if (left) {
					// find next station name
					txtPtr = -1;
					for (j=i; j<lenStr; ++j) {
						p2 = str[j];
						if ((p2.flags&PT_FLG_STATION)!=0) {
							txtPtr = p2.ptr;
							break;
						}
					}
				}
				left = !left;
				// use next (or same points) Name on left points
				// use previous name on right point
				p1.ptr = txtPtr;
			}
		}
		// shorten the list to ES length
		for (i=0; i<lenStr; ++i) {
			p1 = str[i];

			if ((p1.flags&PT_FLG_ES)==0) {
				// we don't need this point in ES mode
				// remove it from the list
				--lenStr;
				for (k=i; k<lenStr; ++k) {
					p1 = str[k];
					p2 = str[k+1];

					p1.melnr = p2.melnr;
					p1.lat = p2.lat;
					p1.lon = p2.lon;
					p1.flags = p2.flags;
					p1.ptr = p2.ptr;
				}
				--i;	// reevaluate moved point
			}
		}
		// in ES mode we load the text strings here
		for (i=0; i<lenStr; ++i) {
			p1 = str[i];
			int idx = p1.ptr;
			if (idx<=0) continue;
			
			int val = ' ';
			int off = 0;
			tmpStr[0].setLength(0);
			for (k=0; k<19; ++k) {
				val = Native.rdMem(DATA_START+idx+k);
				if (val==0 || val=='^') break;
				tmpStr[0].append((char) val);
			}
			p1.stationLine1.setLength(0);
			p1.stationLine2.setLength(0);
			p1.stationLine1.append(tmpStr[0]);
System.out.print(p1.melnr);
Dbg.wr(p1.stationLine1);
System.out.println();
		}
		return lenStr>0;
	}

	/**
	*	Start dial up and
	*	set IP address in Comm.
	*/
	static void startComm() {

		if (Status.strNr<=0) return;

		int cnt = intVal(OFF_CNT);
		int addr = OFF_FIRST;
		for (int i=0; i<cnt; ++i) {
			if (intVal(addr+STR_NR)==Status.strNr) {
				loadString(connStr[0], addr+STR_DIAL);
				loadString(connStr[1], addr+STR_NET);
				loadString(connStr[2], addr+STR_UID);
				loadString(connStr[3], addr+STR_PWD);
				break;
			}
			addr += STR_LEN+intVal(addr+STR_CNT)*PT_LEN;		// find next Strecke
		}
		Comm.startConnection(dstIp, connStr);
Dbg.wr("IP dest: ");
Dbg.intVal((Comm.dst_ip>>>24)&0xff);
Dbg.intVal((Comm.dst_ip>>>16)&0xff);
Dbg.intVal((Comm.dst_ip>>>8)&0xff);
Dbg.intVal((Comm.dst_ip>>>0)&0xff);
Dbg.wr('\n');
for (int i=0; i<4; ++i) {
Dbg.wr('\"');
Dbg.wr(connStr[i]);
Dbg.wr("\"\n");
}
	}

	/**
	*	Check if flash data is valid.
	*/
	public static boolean ok() {

		return (intVal(0)==0x0ebb0ebb);
	}

	public static void check(boolean log) {

		int i, j;
		if (!ok()) {
			System.out.println("Keine Streckendaten");
		}
		
		// System.out.println("Streckendaten:");
		// dump();
		int cnt = intVal(OFF_CNT);
		Dbg.wr("Anzahl: ");
		Dbg.intVal(cnt);
		Dbg.wr('\n');

		i = intVal(BGID_START-DATA_START);
		System.out.print("BG Id: ");
		System.out.println(i);
		System.out.println("Logbook:");
		
		for (i=CONFIG_LEN; i<0x10000; ++i) {
			j = Native.rdMem(BGID_START+i);
			if (j=='\n') {
				if (log) System.out.println();
			} else if (j==0xff) {
				System.out.println("Logbook end");
				break;
			} else {
				if (log) System.out.print((char) j);
			}
			Timer.wd();
		}
		// Move the log data
		// the threshold stand in relation to the copy length
		// in moveLog().
		if (i>45000) {
// System.out.println("move log");
			BgTftp.moveLog();
		}
		
		for (logPtr=CONFIG_LEN; logPtr<0x10000; ++logPtr) {
			j = Native.rdMem(BGID_START+logPtr);
			if (j==0xff) {
				break;
			}
		}

/*
		for (int i=0; i<cnt; ++i) {
			info(i);
			Timer.wd();
		}
*/

	}

	public static int getVer() {
		return intVal(OFF_VER);
	}

	public static int getCnt() {
		return intVal(OFF_CNT);
	}

	public static int getStrNr(int idx) {
		int addr = OFF_FIRST;
		for (int i=0; i<idx; ++i) {
			addr += STR_LEN+intVal(addr+STR_CNT)*PT_LEN;		// find next Strecke
		}
		return intVal(addr+STR_NR);
	}

	public static int getId() {
		return intVal(0+BGID_START-DATA_START);
	}
	public static boolean isMaster() {
		int val = intVal(BG_MASTER+BGID_START-DATA_START);
		return (val==BG_MASTER_MAGIC);
	}
	

	public static void dump() {

		int len = intVal(OFF_LEN);
		// for (int i=0; i<len; i+=4) {
		for (int i=0; i<len; ++i) {
			if ((i&0x03)==0) {
				Dbg.wr('\n');
				Timer.wd();
			}
			Dbg.hexVal(Native.rdMem(DATA_START+i));
			if ((i&0x03)==3) {
				Dbg.intVal(intVal(i&0xfffc));
			}
/*
			int val = intVal(i);
			Dbg.hexVal(val);
			Dbg.intVal(val);
			Dbg.wr('\n');
*/
		}
		Dbg.wr('\n');
	}

	/**
	 * Write logfile entry in the Flash
	 * @param tmpStr2
	 */
	public static void log(StringBuffer str) {
		
		int i, j;
		
		for (i=0; i<str.length(); ++i) {
			j = str.charAt(i);
			System.out.print((char) j);
			if (logPtr<0x10000) {
				Amd.program(BGID_START-0x80000+logPtr, j);
				++logPtr;
			}
			Timer.wd();
		}
		
	}

/*
	public static void info(int nr) {

		int i, j, addr;
		addr = OFF_FIRST;
		for (i=0; i<nr; ++i) {
			addr += STR_LEN+intVal(addr+STR_CNT)*PT_LEN;		// find next Strecke
		}
		int str = intVal(addr+STR_NR);
		Dbg.wr("Nummer: ", str);
		Dbg.wr("IP: ", intVal(addr+STR_IP));
		int cnt = intVal(addr+STR_CNT);
		Dbg.wr("Punkte: ", cnt);

		int melnr = getFirst(str);
		while (melnr!=-1) {

			Point p = getPoint(melnr);
			if (p!=null) {

				Dbg.wr("Melnr: ", p.melnr);
				Dbg.wr("lat: ", p.lat);
				Dbg.wr("long: ", p.lon);
				int idx = p.ptr;
				Dbg.wr("Text: ");
				if (idx<=0) {
					Dbg.intVal(idx);
				} else {
					for (j=0; j<20; ++j) {
						int val = Native.rdMem(DATA_START+idx+j);
						if (val==0) break;
						Dbg.wr(val);
					}
				}
				Dbg.wr('\n');
			}
			melnr = getNext(melnr);
		}

// read direct from Flash

//		addr += STR_LEN;			// start of points
//		for (i=0; i<cnt; ++i) {
//			Dbg.wr("Melnr: ", intVal(addr+PT_MELNR));
//			Dbg.wr("lat: ", intVal(addr+PT_LAT));
//			Dbg.wr("long: ", intVal(addr+PT_LON));
//			int idx = intVal(addr+PT_PTR);
//			addr += PT_LEN;
//			Dbg.wr("Text: ");
//			if (idx<=0) {
//				Dbg.intVal(idx);
//			} else {
//				for (j=0; j<20; ++j) {
//					int val = Native.rdMem(DATA_START+idx+j);
//					if (val==0) break;
//					Dbg.wr(val);
//				}
//			}
//			Dbg.wr('\n');
//		}


		
	}
*/

}
