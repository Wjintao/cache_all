import java.util.Map;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.EasyMemUtil;



public class TestGet {
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			// һ������ȡ
			System.out.println("key=testName1��ֵΪ: " + DefaultEasyMemClient.getInstance().get("testName1"));
			System.out.println("key=testName1��ֵΪ: " + DefaultEasyMemClient.getInstance().get("testName2"));

			// ��ȡ����
			System.out.println("key=testAgeMap��ֵΪ: " + DefaultEasyMemClient.getInstance().get("testAgeMap"));

			// ������ȡ
			Map map1 = DefaultEasyMemClient.getInstance().getMultiArray(new String[] { "testName1", "testName2" });
			System.out.println("key=testName1��testName2��ֵΪ" + map1);
		}
		
	}
}
