package kr.ac.kaist.nlp2cal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Woo on 15. 2. 10..
 */
public class RegularExpression {
    private Hashtable<Character, Character[]> notation;
    private Hashtable<PatternGroup, String> ptnToOrigString;
    private LinkedList<PatternGroup> internalPatterns;

    /**
     * Structure to temporarily contain the matching result.
     * @author DH
     *
     */
    public class MatchingResult implements Serializable {
        public int startPos;
        public int endPos;
        public String senExp;
        public Pattern p;
        public PatternGroup pg;
        public String origExp;
    }

    /**
     * Class to parse the time expression.
     * @author DH
     *
     */
    public class PatternGroup implements Serializable{
        public Pattern totPattern;				// Whole one pattern.
        public Pattern[] partialPattern;		// Each component of the pattern.
        public String[] instructions;			// Meaning of the pattern.
    }

    /**
     * Class to save the idiom pattern.
     * @author DH
     *
     */
    public class IdiomPatternGroup extends PatternGroup implements Serializable{
        public boolean isSolar	= true;
        public int month	= -1;
        public int day		= -1;
        public int weekcnt	= -1;
        public int weekday	= -1;
    }

    /**
     * Class to save the 'every' pattern.
     * @author DH
     */
    public class EveryPatternGroup extends PatternGroup implements Serializable{
        public boolean everyDay		= false;
        public boolean everyWeek	= false;
        public boolean everyMonth	= false;
        public boolean everyYear	= false;
        public boolean[] dayOfWeek	= new boolean[7];
    }

    /**
     * Class to save the relative pattern.
     * @author DH
     */
    public class RelativePatternGroup extends PatternGroup implements Serializable{
        public boolean hasAbsoluteVal	= false;
        public boolean effectiveOnDay	= false;
        public boolean effectiveOnWeek	= false;
        public boolean effectiveOnMonth	= false;
        public boolean effectiveOnYear	= false;
        public boolean afterSchedule	= false;
        public int offset				= -1;
    }

    public RegularExpression(){
        notation			= new Hashtable<Character, Character[]>();
        internalPatterns	= new LinkedList<PatternGroup>();
        ptnToOrigString		= new Hashtable<PatternGroup, String>();
    }

    /**
     * Define new notation.
     * @param newNotation
     * @param literals
     */
    public void addNewNotation(Character newNotation, LinkedList<Character> literals){
        if(notation.get(newNotation) != null){
            notation.remove(newNotation);
        }

        Iterator<Character> litIter	= literals.iterator();
        Character[] litArr			= new Character[literals.size()];
        int cnt						= 0;
        while(litIter.hasNext()){
            litArr[cnt++]			= litIter.next();
        }
        notation.put(newNotation, litArr);
    }

    /**
     * Get a regex pattern, and change it using stored notations to make it as Java processable REGEX pattern.
     * Use default compile flag: COMMENTS
     * @param regex
     * @return Compiled pattern object for java regular expression processing.
     */
    public Pattern changeRegex2JavaProcessablePattern(String regex){
        return changeRegex2JavaProcessablePattern(regex, 0);
    }

    /**
     * Get a regex pattern, and change it using stored notations to make it as Java processable REGEX pattern.
     * @param regex
     * @param compileFlag flag to compile the regular expression pattern.
     * 		CANON_EQ: Enables canonical equivalence.
     *      CASE_INSENSITIVE: Enables case-insensitive matching.
     *      COMMENTS: Permits whitespace and comments in pattern.
     *      DOTALL: Enables dotall mode.
     *      MULTILINE: Enables multiline mode.
     *      UNICODE_CASE: Enables Unicode-aware case folding.
     *      UNIX_LINES: Enables Unix lines mode.
     * @return Compiled pattern object for java regular expression processing.
     */
    public Pattern changeRegex2JavaProcessablePattern(String regex, int compileFlag){
        String resultPattern	= "";
        for(int i = 0; i < regex.length(); i++){
            if(regex.charAt(i) =='\\'){
                resultPattern	+= regex.charAt(i);
                i++;
                if(i < regex.length()){
                    resultPattern	+= regex.charAt(i);
                }
                if(i < regex.length() - 1 && regex.charAt(i + 1) != '+' && regex.charAt(i + 1) != '*'){
                    resultPattern		+= " *";
                }else if(i == regex.length()){
                    resultPattern		+= " *";
                }
                continue;
            }
            Character[] notLiterals	= notation.get(regex.charAt(i));
            if(notLiterals != null){
                resultPattern		+= "(";
                for(int j = 0; j < notLiterals.length - 1; j++){
                    resultPattern	+= notLiterals[j] + "|";
                }
                resultPattern		+= notLiterals[notLiterals.length - 1] + ")";
                resultPattern		+= " *";
                continue;
            }
            resultPattern			+= regex.charAt(i);
            if(i < regex.length() - 1 && regex.charAt(i + 1) != '+' && regex.charAt(i + 1) != '*'){
                resultPattern		+= " *";
            }else if(i == regex.length()){
                resultPattern		+= " *";
            }
        }
        Pattern regPattern			= Pattern.compile(resultPattern, compileFlag);
        return regPattern;
    }

    /**
     * Find positions from which to which the txt matches to the given regex pattern.
     * @param txt target text
     * @param regex Compiled regular expression pattern
     * @return offset of matched parts from the given txt
     */
    public int[] getMatchedText(String txt, Pattern regex){
        LinkedList<Integer> retList	= new LinkedList<Integer>();
        Matcher m					= regex.matcher(txt);

        while(m.find()){
            retList.add(m.start());
            retList.add(m.end());
        }

        int[] ret					= new int[retList.size()];
        Iterator<Integer> retIter	= retList.iterator();
        int cnt						= 0;
        while(retIter.hasNext()){
            ret[cnt++]				= retIter.next();
        }
        return ret;
    }

    /**
     * Match the given text using the internal patterns stored in this object.
     * Returns array of matching result.
     * Only the longest matching result will be maintained.
     * @param txt
     * @return
     */
    public MatchingResult[] matchWithInternalPatterns(String txt){
        if(txt.trim().length()== 0){
            return new MatchingResult[0];
        }
        int[] spaceOffset						= new int[txt.length() + 1];// If extraction offset > key, it must be added with the value of table.
        for(int i = 0; i < spaceOffset.length; i++){
            spaceOffset[i]						= 0;
        }
        txt								= txt.replaceAll("\\(","\\[").replaceAll("\\)", "\\]");
        boolean conseq	= false;
        boolean doubleCon	= false;
        int idx			= 0;
        String real		= "";
//		System.out.println("TEXT: " + txt);
        for(int i = 0; i < txt.length(); i++){
            if(txt.charAt(i) == ' '){
                if(conseq){
                    if(!doubleCon){
                        idx--;
                    }
                    spaceOffset[idx]++;
                    doubleCon	= true;
                }else{
                    conseq	= true;
                    real	+= txt.charAt(i);
                    if(idx > 0){
                        spaceOffset[idx]	= spaceOffset[idx - 1];
                    }else{
                        spaceOffset[idx]	= 0;
                    }
                    idx++;
                }
            }else{
                real	+= txt.charAt(i);
                if(conseq && doubleCon){
                    idx++;
                    doubleCon	= false;
                }
                conseq		= false;

                if(idx > 0){
                    spaceOffset[idx]	= spaceOffset[idx - 1];
                }else{
                    spaceOffset[idx]	= 0;
                }
                idx++;
            }
        }
        spaceOffset[idx]	= spaceOffset[idx - 1];
/*		for(int i = 0; i <= idx; i++){
			System.out.print(spaceOffset[i] + " ");
		}
		System.out.println(idx);
		System.out.println("");*/
        spaceOffset[txt.length()]	= spaceOffset[txt.length() - 1];
        LinkedList<MatchingResult> ret	= new LinkedList<MatchingResult>();
        Iterator<PatternGroup> ptnIter		= internalPatterns.iterator();
        int cnt2	= 0;
        while(ptnIter.hasNext()){
            PatternGroup pg				= ptnIter.next();
            Matcher m					= pg.totPattern.matcher(real);

            while(m.find()){
                int startOffset			= m.start() + spaceOffset[m.start()];
                int endOffset			= m.end()+ spaceOffset[m.end()];

//				System.out.println("FOUND: " + txt.substring(startOffset, endOffset) + "(PATTERN: " + pg.totPattern.pattern() + ")");
//				System.out.println("FOUND: " + txt.substring(startOffset, endOffset));

                Iterator<MatchingResult> retIter	= ret.iterator();
                boolean contain			= false;
                boolean discard			= false;
                while(retIter.hasNext()){
                    MatchingResult mr	= retIter.next();
                    if((mr.startPos >= startOffset && mr.startPos < endOffset) ||
                            (mr.endPos > startOffset && mr.endPos <= endOffset) ||
                            (mr.startPos <= startOffset && mr.endPos >= endOffset)){
                        int origLen		= mr.endPos - mr.startPos;
                        int newLen		= endOffset - startOffset;
                        if(newLen > origLen){

//							System.out.println("CONTAINING EXPRESSION: " +  txt.substring(startOffset, endOffset));
//							System.out.println("CONTAINS: " + mr.startPos + "/" + mr.endPos);


                            contain		= true;
                            mr.startPos	= startOffset;
                            mr.endPos	= endOffset;
                            mr.p		= pg.totPattern;
                            mr.pg		= pg;
                            mr.senExp	= txt.substring(startOffset, endOffset);
                            mr.origExp	= ptnToOrigString.get(pg);

//							System.out.println("NEW: " + startOffset + "/" + endOffset);
                        }else{
                            discard		= true;
//							System.out.println("DISCARDED: " +  txt.substring(startOffset, endOffset));
//							System.out.println("EXISTING: " + mr.startPos + "/" + mr.endPos);
//							System.out.println("NEW: " + startOffset + "/" + endOffset);

                        }
                        break;
                    }
                }
                if(!contain && !discard){
                    MatchingResult newMR	= new MatchingResult();
                    newMR.startPos			= startOffset;
                    newMR.endPos			= endOffset;
                    newMR.p					= pg.totPattern;
                    newMR.pg				= pg;
                    newMR.origExp			= ptnToOrigString.get(pg);
                    newMR.senExp			= txt.substring(startOffset, endOffset);
                    ret.add(newMR);

                }
            }
        }
        Iterator<MatchingResult> retIter	= ret.iterator();
        MatchingResult[] finMR				= new MatchingResult[ret.size()];
        int cnt	= 0;
        while(retIter.hasNext()){
            MatchingResult mr	= retIter.next();
            boolean dup			= false;
            for(int i = 0; i < cnt; i++){
                if(finMR[i].startPos <= mr.startPos && finMR[i].endPos >= mr.endPos){
                    dup	= true;
                    break;
                }
            }
            if(!dup){
                finMR[cnt++]	= mr;
            }
        }

        MatchingResult[] finMRRet			= new MatchingResult[cnt];
        for(int i = 0 ;i < cnt; i++){
            finMRRet[i]						= finMR[i];
//			System.out.println("FINAL RETURN: " + txt.substring(finMRRet[i].startPos, finMRRet[i].endPos));

        }
        return finMRRet;
    }



    /**
     * Generate relative pattern group.
     * @param pattern
     * @param info
     * @return
     */
    public PatternGroup generateRelativePatternGroup(String pattern, String info){
        String[] partialPtns		= pattern.split("<p>");
        RelativePatternGroup rpg	= new RelativePatternGroup();
        rpg.totPattern				= this.changeRegex2JavaProcessablePattern(pattern.replaceAll("<p>", ""));
        StringTokenizer tok			= new StringTokenizer(info, "\t");
        String kind					= tok.nextToken().trim();
        if(kind.equals("+")){
            rpg.afterSchedule		= true;
        }
        String offset				= tok.nextToken().trim();
        if(offset.charAt(0) == '<'){
            rpg.offset				= Integer.parseInt(offset.substring(1, offset.length() - 1));
        }else{
            rpg.hasAbsoluteVal		= true;
            rpg.offset				= Integer.parseInt(offset);
        }

        String effect				= tok.nextToken().trim();
        if(effect.equals("D")){
            rpg.effectiveOnDay		= true;
        }else if(effect.equals("W")){
            rpg.effectiveOnWeek		= true;
        }else if(effect.equals("M")){
            rpg.effectiveOnMonth	= true;
        }else if(effect.equals("Y")){
            rpg.effectiveOnYear		= true;
        }

        rpg.partialPattern		= new Pattern[partialPtns.length];
        for(int i = 0; i < rpg.partialPattern.length; i++){
            rpg.partialPattern[i]	= this.changeRegex2JavaProcessablePattern(partialPtns[i]);
        }

        return rpg;
    }


    /**
     * Generate idiom pattern group.
     * @param pattern
     * @param info
     * @return
     */
    public IdiomPatternGroup generateIdiomPatternGroup(String pattern, String info){
        IdiomPatternGroup ipg	= new IdiomPatternGroup();
        ipg.totPattern			= this.changeRegex2JavaProcessablePattern(pattern.replaceAll(" ", ""));
        StringTokenizer tok		= new StringTokenizer(info, "\t");
        while(tok.hasMoreTokens()){
            String infoTok		= tok.nextToken().trim();
            if(infoTok.equals("L")){
                ipg.isSolar		= false;
            }else if(infoTok.equals("S")){
                ipg.isSolar		= true;
            }else if(infoTok.charAt(0) == 'M'){
                ipg.month		= Integer.parseInt(infoTok.substring(1));
            }else if(infoTok.charAt(0) == 'D'){
                ipg.day			= Integer.parseInt(infoTok.substring(1));
            }else if(infoTok.charAt(0) == 'W'){
                if(infoTok.charAt(1) == 'D'){
                    ipg.weekday	= Integer.parseInt(infoTok.substring(2));
                }else{
                    if(infoTok.charAt(1) == 'L'){
                        ipg.weekcnt		= 5;
                    }else{
                        ipg.weekcnt		= Integer.parseInt(infoTok.substring(1));
                    }
                }
            }
        }

        return ipg;
    }

    /**
     * Generate iterative pattern group.
     * @param pattern
     * @param info
     * @return
     */
    public EveryPatternGroup generateEveryPatternGroup(String pattern, String info){
        EveryPatternGroup epg	= new EveryPatternGroup();
        epg.totPattern			= this.changeRegex2JavaProcessablePattern(pattern.replaceAll(" ", ""));
        for(int i = 0; i < epg.dayOfWeek.length; i++){
            epg.dayOfWeek[i]	= false;
        }
        StringTokenizer tok		= new StringTokenizer(info, "\t");
        while(tok.hasMoreTokens()){
            String infoTok		= tok.nextToken().trim();
            if(infoTok.equals("Y")){
                epg.everyYear	= true;
            }else if(infoTok.equals("M")){
                epg.everyMonth	= true;
            }else if(infoTok.charAt(0) == 'W'){
                epg.everyWeek	= true;
            }else if(infoTok.charAt(0) == 'D'){
                epg.everyDay	= true;
            }else if(infoTok.charAt(0) == 'W' && infoTok.charAt(1) == 'D'){
                for(int i = 2; i < infoTok.length(); i++){
                    epg.dayOfWeek[infoTok.charAt(i) - 49]	= true;
                }
            }
        }

        return epg;
    }

    /**
     * Initialize relative patterns from the given pattern file.
     * @param fileName
     * @throws Exception
     */
    public void initializeRelativePatternFromFile(String fileName){
        internalPatterns.clear();
        notation.clear();
        try{
            BufferedReader in	= new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String str			= "";
            boolean isPattern	= false;
            boolean isNote		= false;
            in.readLine();
            while((str = in.readLine()) != null){
                if(str.equals("<PATTERN>")){
                    isPattern	= true;
                    isNote		= false;
                    continue;
                }
                if(str.equals("<NOTATION>")){
                    isNote		= true;
                    continue;
                }
                if(!isPattern && !isNote){
                    Character nonLit	= str.substring(0, str.indexOf(':')).charAt(0);
                    String literals		= str.substring(str.indexOf(':') + 1);
                    StringTokenizer tok	= new StringTokenizer(literals, ",");
                    Character[] literal	= new Character[tok.countTokens()];
                    int cnt				= 0;
                    while(tok.hasMoreTokens()){
                        literal[cnt++]	= tok.nextToken().charAt(0);
                    }
                    notation.put(nonLit, literal);
                }else if(isPattern && !isNote){
                    str					= str.replaceAll("\\[", "\\\\[");
                    str					= str.replaceAll("\\]", "\\\\]");
                    str					= str.replaceAll("\\.", "\\\\.");

                    PatternGroup pg		= this.generateRelativePatternGroup(str, in.readLine());
                    internalPatterns.add(pg);
                    ptnToOrigString.put(pg, str);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Initialize idiom patterns from the given pattern file.
     * @param fileName
     * @throws Exception
     */
    public void initializeIdiomPatternFromFile(String fileName){
        internalPatterns.clear();
        notation.clear();
        try{
            BufferedReader in	= new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String str			= "";
            boolean isPattern	= false;
            boolean isNote		= false;
            in.readLine();
            while((str = in.readLine()) != null){
                if(str.equals("<PATTERN>")){
                    isPattern	= true;
                    isNote		= false;
                    continue;
                }
                if(str.equals("<NOTATION>")){
                    isNote		= true;
                    continue;
                }
                if(isPattern && !isNote){
                    PatternGroup pg		= this.generateIdiomPatternGroup(str, in.readLine());
                    internalPatterns.add(pg);
                    ptnToOrigString.put(pg, str);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Initialize every patterns from the given pattern file.
     * @param fileName
     * @throws Exception
     */
    public void initializeEveryPatternFromFile(String fileName){
        internalPatterns.clear();
        notation.clear();
        try{
            BufferedReader in	= new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String str			= "";
            boolean isPattern	= false;
            boolean isNote		= false;
            in.readLine();
            while((str = in.readLine()) != null){
                if(str.equals("<PATTERN>")){
                    isPattern	= true;
                    isNote		= false;
                    continue;
                }
                if(str.equals("<NOTATION>")){
                    isNote		= true;
                    continue;
                }
                if(isPattern && !isNote){
                    PatternGroup pg		= this.generateEveryPatternGroup(str, in.readLine());
                    internalPatterns.add(pg);
                    ptnToOrigString.put(pg, str);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
