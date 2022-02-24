package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.schema.exchange.movement.mobileterminal.v1.IdList;
import fish.focus.schema.exchange.movement.mobileterminal.v1.IdType;
import fish.focus.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import fish.focus.schema.exchange.movement.v1.*;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;

public class InterpreterTest {


    @Test
    public void interpreterTest() throws Exception{
        //Containing a couple of messages with one error        NOTE:Old IP
        String base64String = "RE5JRCAgMTA4OTEgMw0NClJldHJpZXZpbmcgRE5JRCBkYXRhLi4uDQoBVCZUARaYDgYAAsQUADFuSV2LKsoCTpVoLOkLGYQAAwAAhYAAAAAAAAABVCZUARaf0wUAAsQUADpuSV2LKtYCTpsoLRoLGYQAVYAAhYAAAAAAAAABVCZUARbxigEAAsQUADpuSV2LKoMCTmO4LvgLGYQATwAAhYAAAAAAAAABVCZUARZqJgEAAsQUADpuSV2LKs0CTpJ4KsILGYQKpAAAhYAAAAAAAAABVCZUARa4yAcAAsQUAEtuSV2LKm4CTmMYSwMLGYQAnoAAhYAAAAAAAAABVCZUARa7dg0CxBQAmW5JXYsq4AJOlggq/AsZgw2pgACFgAAAAAAAAAFUJlQBFrRNBQACxBQAs25JXYsqkQJOo1grKwsZhAkCgACFgAAAAAAAAA0KPiA=";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }

    @Test
    public void interpreterTest2() throws Exception{
        //Containing more messages including one with an error      NOTE:Old IP
        String base64String = "RE5JRCAgMTA4OTAgMw0NClJldHJpZXZpbmcgRE5JRCBkYXRhLi4uDQoBVCZUARah6wcAAsQUAAaJSV2KKqgCTniwK2sLGcIMa4AAAAAAAAAAAAABVCZUARZAzAQAAsQUAA+JSV2KKt0CTkygL6WLGcINIQAAhYAAAAAAAAABVCZUARYYmwQAAsQUAA+JSV2KKqYCTmyoQoYLGcIAsQAAAAAAAAAAAAABVCZUARaT+w0CxBQAD4lJXYoqvAJOY7gu94sZwQA2AACFgAAAAAAAAAFUJlQBFqVFAwACxBQAD4lJXYoq2AJORUAwAQsZwgAjgACFgAAAAAAAAAFUJlQBFgm5BQACxBQAF4lJXYoqiAJOCZA+IgsZwgChgACFgAAAAAAAAAFUJlQBFq3nBwACxBQAGIlJXYoqxgJOS6Avq4sZwgwGAACFgAAAAAAAAAFUJlQBFoaOBAACxBQAIIlJXYoqDQJOlWAs3AsZwgBNgACFgAAAAAAAAAFUJlQBFu7KAAACxBQAIIlJXYoqzQJOlgAqqYsZwgkEgACFgAAAAAAAAAFUJlQBFu84DwACxBQAIIlJXYoqmAJN4Ug5VosZwgCtAACFgAAAAAAAAAFUJlQBFpu7DQLEFAAgiUldiiq0Ak5qUC54ixnCAH2AAIWAAAAAAAAAAVQmVAEWG1YAAALEFAApiUldiiokAk2aKEFxCxnCAKKAAIWAAAAAAAAAAVQmVAEWCz4OAALEFAApiUldiip0Ak54UC4pixnCAKmAAIWAAAAAAAAAAVQmVAEWVT4FAALEFAApiUldiiprAk+xsEdGixnCAKiAAIWAAAAAAAAAAVQmVAEWxuYEAALEFAApiUldiiqZAk4AGDq7CxnCAK0AAIWAAAAAAAAAAVQmVAEWnbwDAALEFAApiUldiirMAk5DUDD8CxnCJiEAAAAAAAAAAAAAAVQmVAEWgQcEAALEFAAyiUldiiq+Ak6OYCynixnCDAEAAIWAAAAAAAAAAVQmVAEWk5EFAALEFAAyiUldiiqDAk5juC74CxnCAE8AAIWAAAAAAAAAAVQmVAEWH0AMAALEFAAyiUldiio8Ak5ysEr0CxnCAHCAAIWAAAAAAAAAAVQmVAEWAloDAALEFAAyiUldiirKAk6VaCzpCxnCAAMAAIWAAAAAAAAAAVQmVAEWn3kCAALEFAAyiUldiiqHAk5uQC0qixnCDGMAAIWAAAAAAAAAAVQmVAEW/TgDAALEFAAyiUldiiq7Ak5GIC9wCxnCEE+AAIWAAAAAAAAAAVQmVAEWQFcAAALEFAA6iUldiipuAk5jGEsDCxnCAJ6AAIWAAAAAAAAAAVQmVAEWmtgBAALEFAA6iUldiiqsAk5owC5sCxnCAJWAAIWAAAAAAAAAAVQmVAEW8BsNAsQUADqJSV2KKhICTmaQLzYLGcIAsgAAhYAAAAAAAAABVCZUARaudAMAAsQUAEOJSV2KKpYCTosAKjaLGcINVgAAhYAAAAAAAAABVCZUARYHFAQAAsQUAEOJSV2KKmQCTmhQLnsLGcIAroAAhYAAAAAAAAABVCZUARaXLgQAAsQUAEOJSV2KKikCTkpISdULGcIAcIAAhYAAAAAAAAABVCZUARaUeQIAAsQUAEuJSV2KKgsCTgmIPiCLGcIAZ4AAhYAAAAAAAAABVCZUARa0awAAAsQUAFSJSV2KKpICTqlALPSLGcIArgAAhYAAAAAAAAABVCZUARYiTQAAAsQUAF2JSV2KKrcCTnEgLbYLGcEMWwAAhYAAAAAAAAABVCZUARap6QgAAsQUAGWJSV2KKigCTkcwIjwLGcIAqIAAhYAAAAAAAAABVCZUARYihQMAAsQUAGWJSV2KKicCTlwQGueLGcIOjoAAhYAAAAAAAAANCj4g";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        List<SetReportMovementType> reportList = new ArrayList<>();
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            reportList.add(convertToMovement(inmarsatMessagesPerOceanRegion[i]));
        }

        for (SetReportMovementType report : reportList) {
            if(report.getMovement().getMobileTerminalId().getMobileTerminalIdList().get(1).getValue().equals("18")){
                assertNotEquals(-164d, report.getMovement().getPosition().getLongitude(), 150d);      //MovementPoint[longitude=-164.05333333333334,latitude=9.241333333333333,altitude=0.0]
                assertNotEquals(9d, report.getMovement().getPosition().getLatitude(), 30d);
                assertEquals("11", report.getMovement().getStatus());
            }
        }

    }

    @Test
    @Ignore
    public void interpreterTest3() throws Exception{
        //One message with the error, NOTE: this string is already padded with the insert missing data method in InmarsatInterpreter    NOTE:Old IP
        String base64String = "AVQmVAEW8BsNAALEFAA6iUldiioSAgJOZpAvNgsZwgCyAACFgAAAAAAA";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        SetReportMovementType report = new SetReportMovementType();
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            report = convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

        assertNotEquals(-164d, report.getMovement().getPosition().getLongitude(), 0.9d);      //MovementPoint[longitude=-164.05333333333334,latitude=9.241333333333333,altitude=0.0]
        assertNotEquals(9d, report.getMovement().getPosition().getLatitude(), 0.9d);

    }

    @Test
    public void interpreterTest4() throws Exception{
        //One message with the error, hopefully not padded but god knows why it is longer then the other string NOTE:Old IP
        String base64String = "RE5JRCAgMTA4OTAgMw0NClJldHJpZXZpbmcgRE5JRCBkYXRhLi4uDQoBVCZUARbwGw0CxBQAOolJXYoqEgJOZpAvNgsZwgCyAACFgAAAAAAAAA0KPiAg";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        SetReportMovementType report = new SetReportMovementType();
        int n = inmarsatMessagesPerOceanRegion.length;
        assertTrue(n == 1);
        for (int i = 0; i < n; i++) {
            report = convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

        assertNotEquals(-164d, report.getMovement().getPosition().getLongitude(), 0.9d);      //MovementPoint[longitude=-164.05333333333334,latitude=9.241333333333333,altitude=0.0] <- current wrong result
        assertNotEquals(9d, report.getMovement().getPosition().getLatitude(), 0.9d);

        assertEquals(11.858d, report.getMovement().getPosition().getLongitude(), 0.01d);        //correct result: latitude=57.64533333333333 longitude=11.858
        assertEquals(57.6453d, report.getMovement().getPosition().getLatitude(), 0.01d);
        assertEquals("11", report.getMovement().getStatus());

    }

    @Test
    public void interpreterTest5() throws Exception{
        //        NOTE : New IP
        //String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEWUh4FAALEFADg2EpdiyrMAk5HEC9qCx28Dk0AAAAAAAAAAAAADQo+IA==";
        String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEWLA4BAALEFADH20pdiSodAk6LmCz7ix3EIxIAAAAAAAAAAAAAAVQmVAEW6bQEAALEFADQ20pdiSoDAk43mDFRix3EADCAAAAAAAAAAAAAAVQmVAEWW68EAALEFADZ20pdiSoyAk61UCuCCx3EJRwAAIWAAAAAAAAAAVQmVAEWvDQHAALEFADZ20pdiSoQAk6QYC2lix3EAEKAAIWAAAAAAAAAAVQmVAEWsqIBAALEFADZ20pdiSoNAk5qUC52Cx3EAIoAAIWAAAAAAAAAAVQmVAEWKScHAALEFADh20pdiSoeAk6EKC3g7x3EACMAAIWAAAAAAAAAAVQmVAEWXuoGAALEFADp20pdiSoFAk5iUC7oCx3EAEOAAAAAAAAAAAAAAVQmVAEWFuoDAALEFADp20pdiSpDAk5DcDEFix3EAFMAAIWAAAAAAAAAAVQmVAEWiqwOAALEFADq20pdiSoxAk66oCxSCx3EJFYAAIWAAAAAAAAAAVQmVAEWNzgEAALEFADq20pdiSovAk61eCwrix3EADIAAIWAAAAAAAAAAVQmVAEWt/IDAALEFADy20pdiSoGAk5vuC0yix3ED1KAAIWAAAAAAAAAAVQmVAEW7FEHAALEFADy20pdiSowAk64ICyiix3EAHyAAIWAAAAAAAAAAVQmVAEWzW4AAALEFADy20pdiSoLAk5moC82Cx3EAGwAAAAAAAAAAAAAAVQmVAEWrLsEAALEFADy20pdiSo0Ak6nKCyGCx3EI3eAAIWAAAAAAAAAAVQmVAEWZycAAALEFAD720pdiSo1Ak6VcCzcCx3EAA8AAIWAAAAAAAAAAVQmVAEWXwEFAALEFAD720pdiSomAk3jKDN0ix3EAAoAAIWAAAAAAAAAAVQmVAEWILsLAALEFAD720pdiSosAk6pQCz0ix3EAGgAAIWAAAAAAAAAAVQmVAEWvgANAsQUAAPcSl2JKjkCTeFIOVaLHcQAXIAAAAAAAAAAAAABVCZUARZy3wQAAsQUAAPcSl2JKjMCTrhwLJeLHcQAZwAAhYAAAAAAAAABVCZUARbfiwIAAsQUAC/cSl2JKgcCTo64LMsLHcQOsYAAhYAAAAAAAAANCj4g";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }

    @Test
    public void interpreterTest6() throws Exception{
        //        NOTE : New IP, from plugin failed report
        //String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEWUh4FAALEFADg2EpdiyrMAk5HEC9qCx28Dk0AAAAAAAAAAAAADQo+IA==";
        String base64String = "AVQmVAEWAwACAMQUAKYAfkZdiyp6Ak4CaMAucAsQ1QBAgACFgAAAAAAAAAFUJlQBFnhuAwACxBQApn5GXYsq3wJOqUAs9IsQ1QAYgACFgAAAAAAAAAFUJlQBFtEbBQACxBQApn5GXYsqtAJOalAudgsQ1QCPAACFgAAAAAAAAAFUJlQBFmyFBgACxBQApn5GXYsqlAJOqqgs2osQ1QBRgACFgAAAAAAAAAFUJlQBFgBjCQACxBQApn5GXYsqvQJOaGAuewsQ1QCwAACFgAAAAAAAAAFUJlQBFhOYAQACxBQApn5GXYsqkQJOqUAs9IsQ1QCkAACFgAAAAAAAAAFUJlQBFnO3BAACxBQAr35GXYsqJwJOZogvNosQ1QBPAACFgAAAAAAAAAFUJlQBFtYMBAACxBQAr35GXYsqmAJN4Ug5VosQ1QCUgACFgAAAAAAAAAFUJlQBFpdFBQACxBQAr35GXYsqlQJOqUAs9IsQ1QCbAACFgAAAAAAAAAFUJlQBFhHiAAACxBQAr35GXYsqUgJOqSgs9QsQ1QBRgACFgAAAAAAAAAFUJlQBFsF7AAACxBQAr35GXYsqpAJOQ3AxBIsQ1QBjAACFgAAAAAAAAAFUJlQBFgQhDwACxBQAr35GXYsqTwJOQ2gxAosQ1QCEAACFgAAAAAAAAAFUJlQBFlc4AwACxBQAr35GXYsqEQJOqIgAnG8Q1TlbAACFgAAAAAAAAAFUJlQBFvJiAQACxBQAr35GXYsqzwJOsAgsm4sQ1QAQgACFgAAAAAAAAAFUJlQBFu7nBQACxBQAt35GXYsqzQJOaLgua4sQ1QCIAACFgAAAAAAAAAFUJlQBFodyAQACxBQAt35GXYsqxgJOTlAwaYsQ1QCcgACFgAAAAAAAAAFUJlQBFhE/BQACxBQAt35GXYsqZAJOaFAuewsQ1QCugACFgAAAAAAAAAFUJlQBFiaaAAACxBQAwH5GXYsqiAJOCZA+IYsQ1QCTgACFgAAAAAAAAAFUJlQBFtHjAgACxBQA0X5GXYsqgwJOY6Au84sQ1QB2gACFgAAAAAAAAAFUJlQBFsGoBAACxBQA635GXYsqrAJOaMAubAsQ1QCkAACFgAAAAAAAAAFUJlQBFvaMDgACxBQA635GXYsqCwJOCYg+IIsQ1QAYAACFgAAAAAAAAAFUJlQBFmc8AAACxBQA9H5GXYsqawJPsbBHRosQ1QCogACFgAAAAAAAAAFUJlQBFrtuAAACxBQA/H5GXYsqugJOaFAueosQ1QCpAACFgAAAAAAAAAFUJlQBFlX7AgACxBQA/H5GXYsqEgJOZpAvNgsQ1QCkAACFgAAAAAAAAA0KPiA=";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }

    @Test
    public void interpreterTest7() throws Exception{
        //        NOTE : New IP, including one message with the error
        String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEW7AgJAALEFACusktdiiq7Ak5KWC+biyCrC16AAIWAAAAAAAAAAVQmVAEWsMsEAALEFAC2sktdiiqYAk3hSDlWiyCrAFmAAIWAAAAAAAAAAVQmVAEW4z4EAALEFAC+sktdiip6Ak5qgC0kiyCrNDIAAIWAAAAAAAAAAVQmVAEWwmoBAALEFAC/sktdiiolAk6tQCyoiyCrACSAAIWAAAAAAAAAAVQmVAEW9ncFAALEFADHsktdiipFAk6EYChWCyCrDiEAAIWAAAAAAAAAAVQmVAEWwH8NAsQUAMeyS12KKk8CTkmQMACLIKsMAYAAhYAAAAAAAAABVCZUARamVggAAsQUANiyS12KKtYCTpsoLRoLIKsAL4AAhYAAAAAAAAANCj4g";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }

    @Test
    public void interpreterTest8() throws Exception{
        //        NOTE : not a valid header from error queue, to see if it works wo header. Missing/missplaced EoH
        String base64String = "AVQmVAEW0WQEAALEFABMXYgqPQJO2DBLRwsiZQBLAACFgAAAAAAAAA0KPiA=";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }


    //half padded hex messages with incomplete headers:
    /*
        0154265401162ab3000002c41400ba5d8b2abe024e95302cd98b215b0069800085800000000000000d0a3e20     base64:    AVQmVAEWKrMAAALEFAC6XYsqvgJOlTAs2YshWwBpgACFgAAAAAAAAA0KPiA=
        0154265401160d9e0a0002c41400cb5d8a2aeb024e79182dda0b215b3093000085800000000000000d0a3e20     base64:    AVQmVAEWDZ4KAALEFADLXYoq6wJOeRgt2gshWzCTAACFgAAAAAAAAA0KPiA=
        015426540116f41e040002c41400985d8a2ae9024e6c3823110b215a1621000085800000000000000154265401160d9e0a0002c41400cb5d8a2aeb024e79182dda0b215b3093000085800000000000000d0a3e20    base64: AVQmVAEW9B4EAALEFACYXYoq6QJObDgjEQshWhYhAACFgAAAAAAAAAFUJlQBFg2eCgACxBQAy12KKusCTnkYLdoLIVswkwAAhYAAAAAAAAANCj4g    NOTE: two messages, two errors
        015426540116456d0000024414004b5d8b2a20024e6b202a410b214f008500008580000000000000015426540116166e04000244140010fa4b5d8b2a30024e95282ab78b214f08618000858000000000000001542654011635300f000244140010fa4b5d8b2adb024e4238303b0b214f0c98800085800000000000000d0a3e20    base64: AVQmVAEWRW0AAAJEFABLXYsqIAJOayAqQQshTwCFAACFgAAAAAAAAAFUJlQBFhZuBAACRBQAEPpLXYsqMAJOlSgqt4shTwhhgACFgAAAAAAAAAFUJlQBFjUwDwACRBQAEPpLXYsq2wJOQjgwOwshTwyYgACFgAAAAAAAAA0KPiA=  NOTE:three messages, one error
        015426540116d164040002c414004c5d882a3d024ed8304b470b2265004b000085800000000000000d0a3e20     base64:    AVQmVAEW0WQEAALEFABMXYgqPQJO2DBLRwsiZQBLAACFgAAAAAAAAA0KPiA=
     */


    @Test
    public void interpreterTest9() throws Exception{
        //        NOTE : from the error queue for the error with a lot of zeros
        String base64String = "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";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

    }

    @Test
    public void interpreterTest10() throws Exception{
        //        NOTE : the message producing a lot of zeros
        String base64String = "AVQmVAEW4QACxBQA1WZQXYoq7wJOaQAua4suYQALAACFgAAAAAAAAA==";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        SetReportMovementType report = new SetReportMovementType();
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            report = convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }

        assertEquals(11.648d, report.getMovement().getPosition().getLongitude(), 0.01d);        //correct result: latitude=57.64533333333333 longitude=11.858
        assertEquals(57.683d, report.getMovement().getPosition().getLatitude(), 0.01d);
        assertEquals("11", report.getMovement().getStatus());

    }

    @Test
    public void interpreterTest11() throws Exception{
        //        NOTE : from plugin failed queue
        //String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEWfzoIAALEFACbbVFdiypOargvpUAxsQAIgACFgAAAAAAAAA0KPiA=";
        String base64String = "DQpSZXRyaWV2aW5nIEROSUQgZGF0YS4uLg0KAVQmVAEWpkwJAALEFADpdVFdiipOargvpQsxxgBEgACFgAAAAAAAAAFUJlQBFqk8BgACxBQAHXZRXYoqdAJOeFAuKYsxxgCpAACFgAAAAAAAAA0KPiA=";

        InmarsatInterpreter inmarsatInterpreter = new InmarsatInterpreter();

        byte[] decodedString = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(decodedString);
        SetReportMovementType report = new SetReportMovementType();
        int n = inmarsatMessagesPerOceanRegion.length;
        for (int i = 0; i < n; i++) {
            report = convertToMovement(inmarsatMessagesPerOceanRegion[i]);
        }


    }



    //This is an almost carbon copy of code in the msgToQue method in the InmarsatMessageListener class. Placed here since I could not get tests to work over there........
    private SetReportMovementType convertToMovement(InmarsatMessage msg) throws Exception{
        MovementBaseType movement = new MovementBaseType();
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();
        IdList dnidId = new IdList();
        dnidId.setType(IdType.DNID);
        dnidId.setValue(Integer.toString(msg.getHeader().getDnid()));
        IdList membId = new IdList();
        membId.setType(IdType.MEMBER_NUMBER);
        membId.setValue(Integer.toString(msg.getHeader().getMemberNo()));

        mobTermId.getMobileTerminalIdList().add(dnidId);
        mobTermId.getMobileTerminalIdList().add(membId);
        movement.setMobileTerminalId(mobTermId);
        movement.setMovementType(MovementTypeType.POS);
        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(((PositionReport) msg.getBody()).getLatitude().getAsDouble());
        mp.setLongitude(((PositionReport) msg.getBody()).getLongitude().getAsDouble());
        movement.setPosition(mp);

        movement.setPositionTime(((PositionReport) msg.getBody()).getPositionDate().getDate());
        movement.setReportedCourse((double) ((PositionReport) msg.getBody()).getCourse());
        movement.setReportedSpeed(((PositionReport) msg.getBody()).getSpeed());
        movement.setSource(MovementSourceType.INMARSAT_C);
        movement.setStatus(Integer.toString(((PositionReport) msg.getBody()).getMacroEncodedMessage()));

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);
        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        reportType.setTimestamp(gcal.getTime());
        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);


        String xml = sendMovementReportToExchange(reportType);

        byte[] combined = new byte[msg.getHeader().header.length + msg.getBody().body.length];

        System.arraycopy(msg.getHeader().header,0,combined,0         ,msg.getHeader().header.length);
        System.arraycopy(msg.getBody().body,0,combined,msg.getHeader().header.length,msg.getBody().body.length);

        String base64Encodede = Base64.getEncoder().encodeToString(combined);

        System.out.println(xml);
        xml.isEmpty();

        return reportType;
    }

    private String sendMovementReportToExchange(SetReportMovementType reportType) {
        String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE", null, Instant.now(), PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
        return text;
    }
}
