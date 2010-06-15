package iubio.readseq;

public interface BioseqDocVals
{
		// some common fields among formats
		// !! update getBioseqdocProperties() below if changes are made here
		// see also BioseqDoc.properties
		
	public final static int 
		kUnknown = 0,  // for extras ...
		kBioseqSet=1, 
		kBioseq=2,  
		kBioseqDoc=3,
		kName = 10, // == ID in general
		kDivision= 11, // databank division: INV, UNA, etc... db specific
		kDataclass= 12, // data class: standard, preliminary, unannotated, backbone
		kDescription = 20,
		kAccession = 30,
		kNid = 31,
		kVersion = 32,
		kKeywords = 40,
		kSource = 50,
		kTaxonomy = 51,
		kReference = 60,
		kAuthor = 61,
		kTitle = 62,
		kJournal = 63,
		kRefCrossref = 64,
		kRefSeqindex = 65,
			
			// various feature flds
		kFeatureTable = 70,
		kFeatureItem = 71,
		kFeatureNote = 72,
		kFeatureKey= 73,
		kFeatureValue=74,
		kFeatureLocation=75,  

		kDate = 80,
		kCrossRef = 90,
		kComment = 100,
		
			// sequence fields
		kSeqstats = 110, // base counts, length, ...
		kSeqdata = 111,
		kSeqlen = 112,
		kSeqkind= 113, //DNA, RNA, tRNA, rRNA, mRNA, uRNA
		kChecksum= 114,
		kSeqcircle= 115, // circular (linear default)
		kStrand= 116, 	// ds, ss, ms
		kNumA= 117, kNumC= 118, kNumG= 119, kNumT = 120, kNumN= 121, // kSeqStats breakdown
		kBlank = 200
			;

			// flags for parsing location in doc
	public final static int 
		kBeforeFeatures = 0,  kAtFeatureHeader= 1, kInFeatures = 2, kAfterFeatures= 3;

			// flags for general doc field kind
	public final static int 
		kField = 1, kSubfield = 2, kContinue = 3, 
		kFeatField= 4, kFeatCont = 5, kFeatWrap= 6;
}