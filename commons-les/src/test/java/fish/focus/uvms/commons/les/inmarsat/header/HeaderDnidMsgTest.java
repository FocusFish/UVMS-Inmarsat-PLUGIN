package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.InmarsatDefinition;
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
public class HeaderDnidMsgTest {
    private final int dataLength;
    private final byte[] header;
    private final InmarsatHeader headerDnidMsg;

    private final HeaderDataPresentation dataPresentation;
    private final int dnid;
    private final int mesNo;
    private final String storedTime;
    private final int lesId;


    public HeaderDnidMsgTest(String headerHex, int dataLength, HeaderDataPresentation dataPresentation, int dnid,
                             String storedTime, int mesNo, int lesId) throws InmarsatException {
        header = InmarsatUtils.hexStringToByteArray(headerHex);
        headerDnidMsg = InmarsatHeader.createHeader(header);
        this.dataLength = dataLength;
        this.dataPresentation = dataPresentation;
        this.dnid = dnid;
        this.storedTime = storedTime;
        this.mesNo = mesNo;
        this.lesId = lesId;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"01542654021993c30c00014c20003f37364ade403521591d02", 32, HeaderDataPresentation.IA5, 16606,
                        "2009-06-15 11:57:51", 492380469, 112},
                {"015426540219890b08000205160036372455c3073521591d02", 22, HeaderDataPresentation.TRANS_DATA, 1987,
                        "2015-04-07 19:59:50", 492380469, 5},
                {"015426540219890b080002e7010036372855c3073521591d02", 1, HeaderDataPresentation.TRANS_DATA, 1987,
                        "2015-04-10 20:48:54", 492380469, 339},
                {"015426540219890b080002A7010036372855c3073521591d02", 1, HeaderDataPresentation.TRANS_DATA, 1987,
                        "2015-04-10 20:48:54", 492380469, 239}});

    }


    @Test
    public void createHeader() {
        assertNotNull(headerDnidMsg);
        assertEquals(HeaderType.DNID_MSG, headerDnidMsg.getType());
        assertEquals(HeaderType.DNID_MSG.getHeaderLength(), headerDnidMsg.getHeaderLength());
    }

    @Test
    public void createHeaderNegative() {
        byte[] cloneHeader = header.clone();
        cloneHeader[HeaderStruct.POS_TYPE] = 8;

        assertThrows(InmarsatException.class, () -> InmarsatHeader.createHeader(cloneHeader));
    }

    @Test
    public void getSatAndLesId() {
        assertEquals(lesId, headerDnidMsg.getSatIdAndLesId());
    }

    @Test
    public void getDataLength() {
        assertEquals(dataLength, headerDnidMsg.getDataLength());
    }

    @Test
    public void getStoredTime() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(InmarsatDefinition.API_TIMEZONE);
        assertEquals(sdf.parse(storedTime), headerDnidMsg.getStoredTime());
    }

    @Test
    public void getDataPresentation() {
        assertEquals(dataPresentation, headerDnidMsg.getDataPresentation());
    }

    @Test
    public void getDnid() {
        assertEquals(dnid, headerDnidMsg.getDnid());
    }

    @Test
    public void getMesMobNo() {
        assertEquals(mesNo, headerDnidMsg.getMesMobNo());
    }
}
