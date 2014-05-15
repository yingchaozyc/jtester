package org.testng.mustache;

abstract public class BaseChunk {

	protected Model m_model;

	public BaseChunk(Model model) {
		m_model = model;
	}

	@SuppressWarnings("unused")
	protected void p(String string) {
		// 真不知道写成这样是要搞啥 FUCK FIXME
		if (false) {
			System.out.println(string);
		}
	}

	abstract String compose();
}
