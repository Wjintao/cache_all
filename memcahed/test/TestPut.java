import java.util.HashMap;
import java.util.Map;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.EasyMemUtil;


public class TestPut {
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			DefaultEasyMemClient objDefaultEasyMemClient = DefaultEasyMemClient.getInstance();

			// ·ÅÖÃ×Ö·û´®
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName1", "Îâ½õÌÎ");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName2", "sbbb");

			// ·ÅÖÃ¶ÔÏó
			Map map = new HashMap();
			map.put("age", "26Ëê");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testAgeMap", map);
		}
	}
}
