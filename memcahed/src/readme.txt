1����easymem.properties�ŵ���Ŀ��config�ĸ�Ŀ¼��easymem.jar�����빤�̵�����classpath��

2����easymem.properties������ʵ�ʶ�ӦMemcache��IP��ַ�Ͷ˿ڣ�֧�ֶ�����ؾ��⣬�Զ��ŷָ�
#server�б�
server.list=localhost:11211,10.3.3.12:1122

3������easymem.jar�ṩ�Ĺ���������Memcache���ж�ȡ���ͻ���ֻ��Ҫ��������public��
com.asiainfo.easymem.DefaultEasyMemClient
com.asiainfo.easymem.EasyMemUtil

ͳһ�ӿڲ����easymem���÷������ж�ȡ����ȡ���ӷ������£�
3.1�������ݵ�Memcache
public class TestPut {
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			DefaultEasyMemClient objDefaultEasyMemClient = DefaultEasyMemClient.getInstance();
			// �����ַ���
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName1", "����1");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName2", "����2");
			// ���ö���
			Map map = new HashMap();
			map.put("age", "17��");
       		objDefaultEasyMemClient.setKeyAndValue2AllServer("testAgeMap", map);
		}
	}
}


3.2��Memcache��ȡ����
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
