package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatHeader;
import fish.focus.uvms.commons.les.inmarsat.InmarsatUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HeaderDnidTest {

    private final byte[] header;
    private final InmarsatHeader headerDnid;
    private final int dataLength;
    private final HeaderDataPresentation dataPresentation;
    private final int dnid;
    private final int memberNo;
    private final String storedTime;
    private final int lesId;


    public HeaderDnidTest(String headerHex, int dataLength, HeaderDataPresentation dataPresentation, int dnid,
                          String storedTime, int memberNo, int lesId) throws InmarsatException {
        header = InmarsatUtils.hexStringToByteArray(headerHex);
        headerDnid = InmarsatHeader.createHeader(header);
        this.dataLength = dataLength;
        this.dataPresentation = dataPresentation;
        this.dnid = dnid;
        this.storedTime = storedTime;
        this.memberNo = memberNo;
        this.lesId = lesId;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"015426540116890b08000155140036372455c307e702", 20, HeaderDataPresentation.IA5, 1987,
                        "2015-04-07 21:59:50", 231, 121},
                {"015426540116890b08000255160036372455c307e702", 22, HeaderDataPresentation.TRANS_DATA, 1987,
                        "2015-04-07 21:59:50", 231, 121},
                {"015426540116890b08000255010036372855c307e702", 1, HeaderDataPresentation.TRANS_DATA, 1987,
                        "2015-04-10 22:48:54", 231, 121}});
    }

    @Test
    public void createHeader() {
        assertNotNull(headerDnid);
        assertEquals(HeaderType.DNID, headerDnid.getType());
        assertEquals(HeaderType.DNID.getHeaderLength(), headerDnid.getHeaderLength());
    }

    @Test
    public void createHeaderNegative() {
        byte[] cloneHeader = header.clone();
        cloneHeader[HeaderStruct.POS_TYPE] = 8;
        assertThrows(InmarsatException.class, () -> InmarsatHeader.createHeader(cloneHeader));
    }

    @Test
    public void getSatIdAndLesId() {
        assertEquals(lesId, headerDnid.getSatIdAndLesId());
    }

    @Test
    public void getDataLength() {
        assertEquals(dataLength, headerDnid.getDataLength());
    }

    @Test
    public void getStoredTime() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        assertEquals(sdf.parse(storedTime), headerDnid.getStoredTime());
    }

    @Test
    public void getDataPresentation() {
        assertEquals(dataPresentation, headerDnid.getDataPresentation());
    }

    @Test
    public void getDnid() {
        assertEquals(dnid, headerDnid.getDnid());
    }

    @Test
    public void getMemberNo() {
        assertEquals(memberNo, headerDnid.getMemberNo());
    }
}
