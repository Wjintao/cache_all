import java.util.HashMap;
import java.util.Map;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.EasyMemUtil;


public class TestPut {
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			DefaultEasyMemClient objDefaultEasyMemClient = DefaultEasyMemClient.getInstance();

			// �����ַ���
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName1", "�����");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName2", "sbbb");

			// ���ö���
			Map map = new HashMap();
			map.put("age", "26��");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testAgeMap", map);
		}
	}
}
