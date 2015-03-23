package kr.ac.kaist.nlp2cal;

import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Created by Woo on 15. 2. 10..
 */
public class CancelDetector {
    private RegularExpression re;
    private Pattern[] chgCluePtnList;
    private Pattern[] cnlCluePtnList;

    public CancelDetector(){
        super();
        // Set up regular expression dealing object.
        re = new RegularExpression();
        LinkedList<Character> sharpLits	= new LinkedList<Character>();
        for(int i = 48; i <= 57; i++){
            sharpLits.add((char)i);
        }
        re.addNewNotation('#', sharpLits);

        // Generate patterns for chg-clues.
        chgCluePtnList	= new Pattern[changeClue.length];
        for(int i = 0; i < changeClue.length; i++){
            chgCluePtnList[i]	= re.changeRegex2JavaProcessablePattern(changeClue[i]);
        }

        // Generate patterns for cancel-clues.
        cnlCluePtnList = new Pattern[cancelClue.length];
        for(int i = 0; i < cancelClue.length; i++) {
            cnlCluePtnList[i] = re.changeRegex2JavaProcessablePattern(cancelClue[i]);
        }
    }

    /**
     * 자질의 이름과, 그 자질에 대한 값을 리턴한다.
     * @return 이름-자질 맵핑 테이블.
     */
    public boolean isUpdated(String txt) {
        // Check rule ID: 52, 57, 107, 109, 116, 142.
        // Simplified form.
        if( isTheTextContainChgClue(txt)){
            return true;
        }
        return false;
    }

    public boolean isCanceled(String txt) {
        if(isTheTextContainCnlClue(txt)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true, if the text contains one of the cancel-representing clues.
     * @param text
     * @return
     */
    private boolean isTheTextContainCnlClue(String text) {
        text	= text.replaceAll("\t", " ");
        text	= text.replaceAll("　", " ");

        for(int i = 0; i < cnlCluePtnList.length; i++) {
            int[] result = re.getMatchedText(text, cnlCluePtnList[i]);
            if(result.length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true, if the text contains one of the change-representing clues.
     * @param text
     * @return
     */
    private boolean isTheTextContainChgClue(String text){
        text	= text.replaceAll("\t", " ");
        text	= text.replaceAll("　", " ");

        for(int i = 0; i < chgCluePtnList.length; i++){
            int[] result		= re.getMatchedText(text, chgCluePtnList[i]);
            if(result.length > 0){
                return true;
            }
        }
        return false;
    }

    /**
     * 일정의 변경 또는 확정을 나타내는 단서.
     * Development set을 통하여 얻어짐.
     */
    private static String[] changeClue	= {"연기", "변경", "이동", "연장"};
    private static String[] cancelClue = {"취소"};
}
