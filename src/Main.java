// 조장 메세지

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class Main {
	public static void main(String[] args) {
		App app = new App();
		app.start();
	}
}

// Session
// 현재 사용자가 이용중인 정보
// 이 안의 정보는 사용자가 프로그램을 사용할 때 동안은 계속 유지된다.
class Session {
	private Member loginedMember = null;
	private Board currentBoard;

	public Member getLoginedMember() {
		return loginedMember;
	}

	public void setLoginedMember(Member loginedMember) {
		this.loginedMember = loginedMember;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}

	public boolean isLogined() {
		return loginedMember != null;
	}
}

// Factory
// 프로그램 전체에서 공유되는 객체 리모콘을 보관하는 클래스

class Factory {
	private static Session session;
	private static DB db;
	private static BuildService buildService;
	private static ArticleService articleService;
	private static ArticleDao articleDao;
	private static MemberService memberService;
	private static MemberDao memberDao;
	private static Scanner scanner;

	public static Session getSession() {
		if (session == null) {
			session = new Session();
		}

		return session;
	}

	public static Scanner getScanner() {
		if (scanner == null) {
			scanner = new Scanner(System.in);
		}

		return scanner;
	}

	public static DB getDB() {
		if (db == null) {
			db = new DB();
		}

		return db;
	}

	public static ArticleService getArticleService() {
		if (articleService == null) {
			articleService = new ArticleService();
		}

		return articleService;
	}

	public static ArticleDao getArticleDao() {
		if (articleDao == null) {
			articleDao = new ArticleDao();
		}

		return articleDao;
	}

	public static MemberService getMemberService() {
		if (memberService == null) {
			memberService = new MemberService();
		}
		return memberService;
	}

	public static MemberDao getMemberDao() {
		if (memberDao == null) {
			memberDao = new MemberDao();
		}

		return memberDao;
	}

	public static BuildService getBuildService() {
		if (buildService == null) {
			buildService = new BuildService();
		}

		return buildService;
	}
}

// App
class App {
	private Map<String, Controller> controllers;

	// 컨트롤러 만들고 한곳에 정리
	// 나중에 컨트롤러 이름으로 쉽게 찾아쓸 수 있게 하려고 Map 사용
	void initControllers() {
		controllers = new HashMap<>();
		controllers.put("build", new BuildController());
		controllers.put("article", new ArticleController());
		controllers.put("member", new MemberController());
	}

	public App() {
		// 컨트롤러 등록
		initControllers();

		// 관리자 회원 생성
		Factory.getMemberService().join("admin", "admin", "관리자");

		// 공지사항 게시판 생성
		Factory.getArticleService().makeBoard("공지사항", "notice");
		// 자유 게시판 생성
		Factory.getArticleService().makeBoard("자유", "free");

		// 현재 게시판을 1번 게시판으로 선택
		Factory.getSession().setCurrentBoard(Factory.getArticleService().getBoard(1));
		// 임시 : 현재 로그인 된 회원은 1번 회원으로 지정, 이건 나중에 회원가입, 로그인 추가되면 제거해야함
		// Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(1));
	}

	void boardName() {
		System.out.printf("< 접속중인 게시판 : %s 게시판 >\n", Factory.getSession().getCurrentBoard().getName());
	}

	public void start() {
		boardName();
		while (true) {
			System.out.println();
			System.out.printf("명령어 : ");
			String command = Factory.getScanner().nextLine().trim();
			System.out.println();

			if (command.length() == 0) {
				continue;
			} else if (command.equals("exit")) {
				break;
			}

			Request reqeust = new Request(command);

			if (reqeust.isValidRequest() == false) {
				continue;
			}

			if (controllers.containsKey(reqeust.getControllerName()) == false) {
				continue;
			}

			controllers.get(reqeust.getControllerName()).doAction(reqeust);
		}

		Factory.getScanner().close();
	}
}

// Request
class Request {
	private String requestStr;
	private String controllerName;
	private String actionName;
	private String arg1;
	private String arg2;
	private String arg3;

	boolean isValidRequest() {
		return actionName != null;
	}

	Request(String requestStr) {
		this.requestStr = requestStr;
		String[] requestStrBits = requestStr.split(" ");
		this.controllerName = requestStrBits[0];

		if (requestStrBits.length > 1) {
			this.actionName = requestStrBits[1];
		}

		if (requestStrBits.length > 2) {
			this.arg1 = requestStrBits[2];
		}

		if (requestStrBits.length > 3) {
			this.arg2 = requestStrBits[3];
		}

		if (requestStrBits.length > 4) {
			this.arg3 = requestStrBits[4];
		}
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public String getArg3() {
		return arg3;
	}

	public void setArg3(String arg3) {
		this.arg3 = arg3;
	}
}

// Controller
abstract class Controller {
	String name;
	String loginId;
	String loginPw;
	boolean logIdPw;

	abstract void doAction(Request reqeust);
}

class BuildController extends Controller {
	private BuildService buildService;
	private static boolean workStarted;

	BuildController() {
		buildService = Factory.getBuildService();
	}
	
	static {
		workStarted = false;
	}

	public void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("site")) {
			actionSite(reqeust);
		} else if (reqeust.getActionName().equals("startAutoSite")) {
			actionStartAutoSite(reqeust);
		} else if (reqeust.getActionName().equals("stopAutoSite")) {
			actionStopAutoSite(reqeust);
		}
	}
	
	// build site start
		private void actionStartAutoSite(Request reqeust) {
			workStarted = true;
			new Thread(() -> {
				while (workStarted) {
					buildService.buildSite();
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
					}
				}
			}).start();
		}
	
	// build site stop
	private void actionStopAutoSite(Request reqeust) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		workStarted = false;
	}
	
	

	private void actionSite(Request reqeust) {
		buildService.buildSite();
	}
}

class ArticleController extends Controller {
	private ArticleService articleService;
	private Article article;

	ArticleController() {
		articleService = Factory.getArticleService();
	}

	public void setArticleService(ArticleService articleService) {
		this.articleService = articleService;
	}

	public void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("help")) {
			actionHelp(reqeust);
		} else if (reqeust.getActionName().equals("write")) {
			actionWrite(reqeust);
		} else if (reqeust.getActionName().equals("list")) {
			actionList(reqeust);
		} else if (reqeust.getActionName().equals("detail")) {
			actionDetail(reqeust);
		} else if (reqeust.getActionName().equals("modify")) {
			actionModify(reqeust);
		} else if (reqeust.getActionName().equals("delete")) {
			actionDelete(reqeust);
		} else if (reqeust.getActionName().equals("listBoard")) {
			actionBoardList(reqeust);
		} else if (reqeust.getActionName().equals("changeBoard")) {
			actionChange(reqeust);
		} else if (reqeust.getActionName().equals("createBoard")) {
			actionCreate(reqeust);
		} else if (reqeust.getActionName().equals("deleteBoard")) {
			actionDeleteBoard(reqeust);
		} else {
			System.out.println("올바른 명령어를 입력해 주세요.");
		}
	}

	// 명령어 리스트
	private void actionHelp(Request reqeust) {
		System.out.println("========= 명령어 리스트 =========");
		System.out.println("article write : 게시물 추가");
		System.out.println("article list : 게시물 리스트");
		System.out.println("article detail : 게시물 상세보기");
		System.out.println("article modify : 게시물 수정");
		System.out.println("article delete : 게시물 삭제");
		System.out.println("article listBoard : 게시판 리스트");
		System.out.println("article changeBoard : 게시판 변경");
		System.out.println("article createBoard : 게시판 생성");
		System.out.println("article deleteBoard : 게시판 삭제");
		System.out.println("exit : 프로그램 종료");
		System.out.println("=================================");
	}

	// 게시판 변경
	private void actionChange(Request reqeust) {
		System.out.println("== 게시판 변경 ==");
		System.out.println("게시판 번호를 입력해주세요.");
		List<Board> boards = articleService.getAllBoard();
		System.out.println(boards);
		System.out.print("> ");

		int id = Factory.getScanner().nextInt();
		Factory.getScanner().nextLine();
		Factory.getSession().setCurrentBoard(articleService.getBoard(id));

		Board board = articleService.getBoardChange(id);

		if (board != null) {
			System.out.printf("%s 게시판으로 변경되었습니다.\n", board.getName());
		} else {
			System.out.println("해당 번호의 게시판은 존재하지 않습니다.\n");

		}
	}

	// 게시판 생성
	private void actionCreate(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			if (loginedMember.getLoginId().equals("admin")) {
				System.out.println("== 게시판 생성 ==");
				System.out.print("> ");

				System.out.println("생성하실 게시판 이름을 입력해주세요.");
				System.out.print("> ");
				String name = Factory.getScanner().nextLine();

				System.out.println("생성하실 게시판 code를 입력해주세요.");
				System.out.print("> ");
				String code = Factory.getScanner().nextLine();

				Factory.getArticleService().makeBoard(name, code);
				System.out.println("<" + name + "> 게시판 생성이 완료되었습니다.");
			} else {
				System.out.println("게시판 생성은 관리자만 가능합니다.");
			}
		} else {
			System.out.println("로그인 후 이용가능합니다");
		}
	}

	// 게시판 리스트
	private void actionBoardList(Request reqeust) {
		List<Board> board = articleService.getAllBoard();
		System.out.println(board);
	}

	// 게시판 삭제
	private void actionDeleteBoard(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();
		if (loginedMember != null) {
			if (loginedMember.getLoginId().equals("admin")) {
				System.out.println("== 게시판 삭제 ==");
				System.out.println("삭제하실 게시판 번호를 입력해주세요.");
				System.out.print("> ");

				int delNum = Factory.getScanner().nextInt();
				Board board = articleService.getBoard(delNum);

				Factory.getScanner().nextLine();
				if (board != null) {

					articleService.deleteboard(delNum);

				} else {
					System.out.println("해당 번호의 게시판은 존재하지 않습니다.");
					return;
				}
			} else {
				System.out.println("게시판 삭제 관리자만 가능합니다.");
				return;
			}
		} else {
			System.out.println("로그인 후 이용가능합니다");
		}
	}

	// 게시물 리스트 - 게시판별로 리스팅
	private void actionList(Request reqeust) {
		System.out.println("게시판 번호를 입력해주세요.");
		List<Board> board = articleService.getAllBoard();
		System.out.println(board);
		System.out.print("> ");

		int id = Factory.getScanner().nextInt();
		Factory.getScanner().nextLine();

		articleService.getArticleBoard(id);
	}

	// 개시물 추가
	private void actionWrite(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			System.out.println("== 게시물 추가 ==");
			System.out.printf("제목 : ");
			String title = Factory.getScanner().nextLine();
			System.out.printf("내용 : ");
			String body = Factory.getScanner().nextLine();

			// 현재 게시판 id 가져오기
			int boardId = Factory.getSession().getCurrentBoard().getId();

			// 현재 로그인한 회원의 id 가져오기
			int memberId = Factory.getSession().getLoginedMember().getId();
			int newId = articleService.write(boardId, memberId, title, body);

			System.out.printf("%d번 글이 생성되었습니다.\n", newId);
		} else {
			System.out.println("로그인 후 이용가능합니다");
		}
	}

	// 게시물 수정
	private void actionModify(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			System.out.println("== 게시물 수정 ==");
			System.out.println("수정하실 게시물 번호를 입력해주세요.");
			System.out.print("> ");

			int modiNum = Factory.getScanner().nextInt();
			article = articleService.getArticleByNum(modiNum);
			Factory.getScanner().nextLine();

			if (article != null) {
				if (loginedMember.getId() == article.getMemberId()) {
					System.out.println("수정하실 제목과 내용을 입력해주세요.");
					System.out.printf("제목 : ");
					String newTitle = Factory.getScanner().nextLine();
					System.out.printf("내용 : ");
					String newBody = Factory.getScanner().nextLine();

					article.setTitle(newTitle);
					article.setBody(newBody);

					articleService.modify(article);
				} else {
					System.out.println("게시물 수정은 본인만 가능합니다.");
					return;
				}
			} else {
				System.out.println("해당 번호의 게시물은 존재하지 않습니다.");
				return;
			}
		} else {
			System.out.println("로그인 후 이용가능합니다");
		}
	}

	// 게시물 삭제
	private void actionDelete(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			System.out.println("== 게시물 삭제 ==");
			System.out.println("삭제하실 게시물 번호를 입력해주세요.");
			System.out.print("> ");

			int delNum = Factory.getScanner().nextInt();
			article = articleService.getArticleByNum(delNum);
			Factory.getScanner().nextLine();
			if (article != null) {
				if (loginedMember.getId() == article.getMemberId()) {
					articleService.delete(delNum);
				} else {
					System.out.println("게시물 삭제는 본인만 가능합니다.");
					return;
				}
			} else {
				System.out.println("해당 번호의 게시물은 존재하지 않습니다.");
				return;
			}
		} else {
			System.out.println("로그인 후 이용가능합니다");
		}
	}

	// 게시물 상세보기
	private void actionDetail(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			System.out.println("== 게시물 상세보기 ==");
			System.out.println("조회하실 게시물 번호를 입력하세요.");
			System.out.print("> ");

			int detailNum = Factory.getScanner().nextInt();
			article = articleService.getArticleByNum(detailNum);
			Factory.getScanner().nextLine();
			if (article != null) {
				articleService.getArticleDetail(detailNum);
			} else {
				System.out.println("해당 게시물은 존재하지 않습니다.");
			}

		} else {
			System.out.println("로그인 후 이용가능합니다.");
		}
	}
}

class MemberController extends Controller {
	private MemberService memberService;

	MemberController() {
		memberService = Factory.getMemberService();
	}

	public void setMemberService(MemberService memberService) {
		this.memberService = memberService;
	}

	// cmd
	public void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("help")) {
			actionHelp(reqeust);
		} else if (reqeust.getActionName().equals("join")) {
			actionJoin(reqeust);
		} else if (reqeust.getActionName().equals("login")) {
			actionLogin(reqeust);
		} else if (reqeust.getActionName().equals("logout")) {
			actionLogout(reqeust);
		} else if (reqeust.getActionName().equals("whoami")) {
			actionWhoami(reqeust);
		} else {
			System.out.println("올바른 명령어를 입력해 주세요.");
		}
	}

	// 명령어 리스트
	private void actionHelp(Request reqeust) {
		System.out.println("========= 명령어 리스트 =========");
		System.out.println("member join : 회원가입");
		System.out.println("member login : 로그인");
		System.out.println("member logout : 로그아웃");
		System.out.println("member whoami : 현재 로그인 상태 여부");
		System.out.println("exit : 프로그램 종료");
		System.out.println("=================================");
	}

	// 현재 로그인 했는지 안했는지 구별 방법
	private void actionWhoami(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			System.out.println("현재 로그인 상태가 아닙니다.");
		} else {
			System.out.printf("%s님 현재 로그인 상태 입니다.\n", loginedMember.getName());
		}

	}

	// 회원가입
	private void actionJoin(Request reqeust) {
		System.out.println("== 회원가입을 시작합니다. ==");

		while (true) {
			System.out.printf("이름을 입력해주세요 : ");
			name = Factory.getScanner().nextLine();

			if (name.length() < 2) {
				System.out.println("이름을 2자 이상 입력해주세요.");
				continue;
			}
			break;
		}

		while (true) {
			System.out.printf("아이디를 입력해주세요 : ");
			loginId = Factory.getScanner().nextLine();

			boolean ueserId = memberService.getMemberLoginId(loginId);
			if (ueserId == false) {
				System.out.println("이미 존재하는 아이디 입니다.");
				continue;
			}

			if (loginId.length() < 2) {
				System.out.println("아이디는 2자 이상 입력해주세요.");
				continue;
			}
			break;
		}

		while (true) {
			boolean loginPwValid = true;

			while (true) {
				System.out.printf("비밀번호를 입력해주세요 : ");
				loginPw = Factory.getScanner().nextLine();

				if (loginPw.length() < 2) {
					System.out.println("비밀번호를 2자 이상 입력해주세요.");
					continue;
				}
				break;
			}

			while (true) {
				System.out.printf("비밀번호를 다시 한 번 입력해주세요. : ");
				String loginPwConfirm = Factory.getScanner().nextLine();

				if (loginPw.length() < 2) {
					System.out.println("비밀번호를 2자 이상 입력해주세요.");
					continue;
				}
				if (loginPw.equals(loginPwConfirm) == false) {
					loginPwValid = false;
					break;
				}

				break;
			}

			if (loginPwValid) {
				break;
			}

		}

		memberService.join(loginId, loginPw, name);
		System.out.printf("%s님 회원가입이 완료되었습니다.\n", name);
	}

	// 로그인
	private void actionLogin(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			while (true) {
				System.out.println("=========================");
				System.out.print("로그인 ID :");
				loginId = Factory.getScanner().nextLine();

				System.out.print("로그인 PW :");
				loginPw = Factory.getScanner().nextLine();

				Member member = memberService.login(loginId, loginPw);

				if (member != null) {
					System.out.printf("%s님이 로그인하셨습니다.\n", member.getName());
					System.out.println("=========================");
					Factory.getSession().setLoginedMember(member);
					break;
				} else {
					return;
				}
			}

		} else {
			System.out.println("현제 로그인 상태입니다.");
		}
	}

	// 로그아웃
	private void actionLogout(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();
		if (loginedMember != null) {
			System.out.println("==========================");
			System.out.printf("%s님이 로그아웃 하셨습니다.\n", loginedMember.getName());
			System.out.println("==========================");
			Factory.getSession().setLoginedMember(null);
		} else {
			System.out.println("로그인을 먼저 해주세요.");
		}
	}
}

//Service
class BuildService {
	ArticleService articleService;

	BuildService() {
		articleService = Factory.getArticleService();
	}

	public void buildSite() {
		Util.makeDir("site/");
		Util.makeDir("site/site_template/");
		Util.makeDir("site/site_template/article"); // list, detail
		Util.makeDir("site/site_template/home"); // 메인 페이지
		Util.makeDir("site/site_template/part"); // 공통 상단, 하단 템플릿
		Util.makeDir("site/site_template/resource"); // 공통 css, js
		Util.makeDir("site/site_template/stat"); // 통계 페이지

		
		
		// part - head, foot.html
		String head = Util.getFileContents("site/site_template/part/head.html");
		String foot = Util.getFileContents("site/site_template/part/foot.html");
		
		List<Board> boards = articleService.getAllBoard();
		
		for(Board board : boards) {
			head = head.replace("{$boardMenu}", board.getCode() + "-list-1.html");
		}
		

		// article - 각 게시판 별 게시물리스트 페이지 생성 / notice, free..

		for (Board board : boards) {
			String fileName = board.getCode() + "-list-1.html";

			String html = "";

			List<Article> articles = articleService.getArticlesByBoardCode(board.getCode());

			for (Article article : articles) {
				html += "<div><a href=\"" + article.getId() + ".html\">게시물 번호 : " + article.getId() + ", 게시물 제목 : "
						+ article.getTitle() + "</a></div>";
			}

			html = head + html + foot;

			Util.writeFileContents("site/site_template/article/" + fileName, html);
		}

		// article - 게시물 별 파일 생성 / 1.html,2.html..
		List<Article> articles = articleService.getArticles();

		for (Article article : articles) {
			String html = "";
			
			html += "<link rel=\"stylesheet\" href=\"../resource/common.css\" />";
			html += "<div>제목 : " + article.getTitle() + "</div>";
			html += "<div>내용 : " + article.getBody() + "</div>";
			html += "<div><a href=\"" + (article.getId() - 1) + ".html\">이전글</a></div>";
			html += "<div><a href=\"" + (article.getId() + 1) + ".html\">다음글</a></div>";

			html = head + html + foot;

			Util.writeFileContents("site/site_template/article/" + article.getId() + ".html", html);
		}
		
		
	}

}

class ArticleService {
	private ArticleDao articleDao;

	ArticleService() {
		articleDao = Factory.getArticleDao();
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return articleDao.getArticlesByBoardCode(code);
	}

	// 게시판 삭제
	public void deleteboard(int delNum) {
		articleDao.deleteboard(delNum);
	}

	// 게시판 변경
	public Board getBoardChange(int id) {
		return articleDao.getBoardChange(id);
	}

	// 게시판 가져오기
	public List<Board> getAllBoard() {
		return articleDao.getAllBoard();
	}

	// num에 해당하는 게시물 가져오기
	public Article getArticleByNum(int num) {
		return articleDao.getArticleByNum(num);
	}

	public int makeBoard(String name, String code) {
		Board oldBoard = articleDao.getBoardByCode(code);

		if (oldBoard != null) {
			return -1;
		}

		Board board = new Board(name, code);
		return articleDao.saveBoard(board);
	}

	public Board getBoard(int id) {
		return articleDao.getBoard(id);
	}

	// 게시물 작성
	public int write(int boardId, int memberId, String title, String body) {
		Article article = new Article(boardId, memberId, title, body);
		return articleDao.save(article);
	}

	// 게시물 리스트
	public List<Article> getArticles() {
		return articleDao.getArticles();
	}

	// code별 리스트
	public void getArticleBoard(int id) {
		articleDao.getArticleBoard(id);
	}

	// 게시물 수정
	public void modify(Article article) {
		articleDao.save(article);
	}

	// 게시물 삭제
	public void delete(int delNum) {
		articleDao.delete(delNum);
	}

	// 게시물 상세보기
	public void getArticleDetail(int detailNum) {
		articleDao.getArticleDetail(detailNum);
	}

}

class MemberService {
	private MemberDao memberDao;

	MemberService() {
		memberDao = Factory.getMemberDao();
	}

	// 회원가입
	public int join(String loginId, String loginPw, String name) {
		Member oldMember = memberDao.getMemberByLoginId(loginId);

		if (oldMember != null) {
			return -1;
		}

		Member member = new Member(loginId, loginPw, name);
		return memberDao.save(member);
	}

	// 로그인
	public Member login(String loginId, String loginPw) {
		Member loginIn = memberDao.login(loginId, loginPw);
		return loginIn;
	}

	// 아이디 존재여부
	public boolean getMemberLoginId(String loginId) {
		Member oldMember = memberDao.getMemberByLoginId(loginId);
		if (oldMember != null) {
			return false;
		}
		return true;
	}

	public Member getMember(int id) {
		return memberDao.getMember(id);
	}
}

// Dao
class ArticleDao {
	DB db;

	ArticleDao() {
		db = Factory.getDB();
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return db.getArticlesByBoardCode(code);
	}

	// 게시판 삭제
	public void deleteboard(int delNum) {
		db.deleteboard(delNum);
	}

	// 게시판 변경
	public Board getBoardChange(int id) {
		return db.getBoardChange(id);
	}

	// 게시판가 져오기
	public List<Board> getAllBoard() {
		return db.getBoards();
	}

	// 게시물 상세보기
	public void getArticleDetail(int detailNum) {
		db.getArticleDetail(detailNum);
	}

	// num에 해당하는 게시물 가져오기
	public Article getArticleByNum(int num) {
		Article article = db.getArticlerByNum(num);
		return article;
	}

	// code별 리스트
	public void getArticleBoard(int id) {
		db.getArticleBoard(id);
	}

	public Board getBoardByCode(String code) {
		return db.getBoardByCode(code);
	}

	public int saveBoard(Board board) {
		return db.saveBoard(board);
	}

	public int save(Article article) {
		return db.saveArticle(article);
	}

	public Board getBoard(int id) {
		return db.getBoard(id);
	}

	public List<Article> getArticles() {
		return db.getArticles();
	}

	// 게시물 삭제
	public void delete(int delNum) {
		db.delete(delNum);
	}

}

class MemberDao {
	DB db;

	MemberDao() {
		db = Factory.getDB();
	}

	// 로그인
	public Member login(String loginId, String loginPw) {
		Member m = db.getMemberByLoginId(loginId);

		if (m == null) {
			System.out.println("가입되지 않은 회원입니다.");
			return null;
		} else {
			Member d = db.getMemberById(loginId, loginPw);
			return d;
		}
	}

	// 아이디 존재여부
	public Member getMemberByLoginId(String loginId) {
		return db.getMemberByLoginId(loginId);
	}

	public Member getMember(int id) {
		return db.getMember(id);
	}

	// 회원가입
	public int save(Member member) {
		return db.saveMember(member);
	}
}

// DB
class DB {
	private Map<String, Table> tables;

	public DB() {
		String dbDirPath = getDirPath();
		Util.makeDir(dbDirPath);

		tables = new HashMap<>();

		tables.put("article", new Table<Article>(Article.class, dbDirPath));
		tables.put("board", new Table<Board>(Board.class, dbDirPath));
		tables.put("member", new Table<Member>(Member.class, dbDirPath));
	}

	public List<Article> getArticlesByBoardCode(String code) {
		Board board = getBoardByCode(code);
		// free => 2
		// notice => 1

		List<Article> articles = getArticles();
		List<Article> newArticles = new ArrayList<>();

		for (Article article : articles) {
			if (article.getBoardId() == board.getId()) {
				newArticles.add(article);
			}
		}

		return newArticles;
	}

	// 게시판 삭제
	public void deleteboard(int delNum) {
		tables.get("board").delete(delNum);
	}

	// 게시판 변경
	public Board getBoardChange(int id) {
		List<Board> boards = getBoards();
		for (Board board : boards) {
			if (board.getId() == id) {
				return board;
			}
		}
		return null;
	}

	// 게시물 삭제
	public void delete(int delNum) {
		tables.get("article").delete(delNum);
	}

	// 게시물 상세보기
	public void getArticleDetail(int detailNum) {
		List<Article> articles = getArticles();
		for (Article article : articles) {
			if (article.getId() == detailNum) {
				Member member = getMember(article.getMemberId());
				System.out.println("============= " + article.getId() + "번 게시물 =============");
				System.out.printf("게시물 번호 : %d\n", article.getId());
				System.out.printf("게시물 작성날짜 : %s\n", article.getRegDate());
				System.out.printf("게시물 제목 : %s\n", article.getTitle());
				System.out.printf("게시물 내용 : %s\n", article.getBody());
				System.out.printf("게시물 작성자 : %s\n", member.getName());
				System.out.println("======================================");
			}
		}
	}

	// code별 리스트
	public void getArticleBoard(int id) {
		List<Article> articles = getArticles();

		
		for (Article article : articles) {
			if (article.getBoardId() == id) {
				System.out.println(article);
			}
		}
		
		/*
		// num을 기준으로 출력
		if (articles != null) {
			System.out.println("번호 | 제목 | 작성 날짜 | 작성자");
			for (int i = (id - 1) * 10; i < id * 10; i++) {
				if (i >= articles.size()) {
					break;
				}
				Article a = articles.get(i);
				System.out.printf("%02d | %s | %s | %s\n", a.getId(), a.getTitle(), a.getRegDate(), getMember(a.getMemberId()).getName());
			}
		} else {
			System.out.println("게시물이 존재하지 않습니다.");
		}*/

	}

	// mum에 해당하는 게시물이 있는지 판별
	public Article getArticlerByNum(int num) {
		List<Article> articles = getArticles();

		for (Article article : articles) {
			if (article.getId() == num) {
				return article;
			}
		}
		return null;
	}

	// 가입된 아이디인지 판별
	public Member getMemberByLoginId(String loginId) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId)) {
				return member;
			}
		}

		return null;
	}

	// 로그인 할 때 아이디와 비밀번호가 일치하는지 비교
	Member getMemberById(String loginId, String loginPw) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId) && member.getLoginPw().equals(loginPw)) {
				return member;
			}
		}
		System.out.println("아이디 또는 비밀번호가 일치하지 않습니다.");
		return null;
	}

	public List<Member> getMembers() {
		return tables.get("member").getRows();
	}

	public Board getBoardByCode(String code) {
		List<Board> boards = getBoards();

		for (Board board : boards) {
			if (board.getCode().equals(code)) {
				return board;
			}
		}

		return null;
	}

	public List<Board> getBoards() {
		return tables.get("board").getRows();
	}

	public Member getMember(int id) {
		return (Member) tables.get("member").getRow(id);
	}

	public int saveBoard(Board board) {
		return tables.get("board").saveRow(board);
	}

	public String getDirPath() {
		return "db";
	}

	public int saveMember(Member member) {
		return tables.get("member").saveRow(member);
	}

	public Board getBoard(int id) {
		return (Board) tables.get("board").getRow(id);
	}

	public List<Article> getArticles() {
		return tables.get("article").getRows();
	}

	public int saveArticle(Article article) {
		return tables.get("article").saveRow(article);
	}

	public void backup() {
		for (String tableName : tables.keySet()) {
			Table table = tables.get(tableName);
			table.backup();
		}
	}
}

// Table
class Table<T> {
	private Class<T> dataCls;
	private String tableName;
	private String tableDirPath;

	public Table(Class<T> dataCls, String dbDirPath) {
		this.dataCls = dataCls;
		this.tableName = Util.lcfirst(dataCls.getCanonicalName());
		this.tableDirPath = dbDirPath + "/" + this.tableName;

		Util.makeDir(tableDirPath);
	}

	private String getTableName() {
		return tableName;
	}

	public int saveRow(T data) {
		Dto dto = (Dto) data;

		if (dto.getId() == 0) {
			int lastId = getLastId();
			int newId = lastId + 1;
			((Dto) data).setId(newId);
			setLastId(newId);
		}

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);

		return dto.getId();
	};

	private String getRowFilePath(int id) {
		return tableDirPath + "/" + id + ".json";
	}

	private void setLastId(int lastId) {
		String filePath = getLastIdFilePath();
		Util.writeFileContents(filePath, lastId);
	}

	private int getLastId() {
		String filePath = getLastIdFilePath();

		if (Util.isFileExists(filePath) == false) {
			int lastId = 0;
			Util.writeFileContents(filePath, lastId);
			return lastId;
		}

		return Integer.parseInt(Util.getFileContents(filePath));
	}

	private String getLastIdFilePath() {
		return this.tableDirPath + "/lastId.txt";
	}

	public T getRow(int id) {
		return (T) Util.getObjectFromJson(getRowFilePath(id), dataCls);
	}

	public void backup() {

	}

	// 게시물 삭제
	public void delete(int num) {
		String fileNm = getRowFilePath(num);
		File file = new File(fileNm);
		if (file.exists()) {
			if (file.delete()) {
				System.out.println("삭제가 완료되었습니다.");
			}
		} else {
			return;
		}
	}

	List<T> getRows() {
		int lastId = getLastId();

		List<T> rows = new ArrayList<>();

		for (int id = 1; id <= lastId; id++) {
			T row = getRow(id);

			if (row != null) {
				rows.add(row);
			}
		}

		return rows;
	};
}

// DTO
abstract class Dto {
	private int id;
	private String regDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	Dto() {
		this(0);
	}

	Dto(int id) {
		this(id, Util.getNowDateStr());
	}

	Dto(int id, String regDate) {
		this.id = id;
		this.regDate = regDate;
	}
}

class Board extends Dto {
	private String name;
	private String code;

	@Override
	public String toString() {
		return String.format("%d) %s 게시판", getId(), name);
	}

	public Board() {
	}

	public Board(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}

class Article extends Dto {
	private int boardId;
	private int memberId;
	private String title;
	private String body;

	public Article() {

	}

	public Article(int boardId, int memberId, String title, String body) {
		this.boardId = boardId;
		this.memberId = memberId;
		this.title = title;
		this.body = body;
	}

	@Override
	public String toString() {
		return String.format("== Article List ==\n게시물 번호 : %d\n제목 : %s\n작성날짜 : %s\n작성자 번호 : %d\n", getId(), title, getRegDate(), getMemberId());
	}

	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}

class ArticleReply extends Dto {
	private int id;
	private String regDate;
	private int articleId;
	private int memberId;
	private String body;

	ArticleReply() {

	}

	public int getArticleId() {
		return articleId;
	}

	public void setArticleId(int articleId) {
		this.articleId = articleId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}

class Member extends Dto {
	private String loginId;
	private String loginPw;
	private String name;

	public Member() {

	}

	public Member(String loginId, String loginPw, String name) {
		this.loginId = loginId;
		this.loginPw = loginPw;
		this.name = name;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginPw() {
		return loginPw;
	}

	public void setLoginPw(String loginPw) {
		this.loginPw = loginPw;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

// Util
class Util {
	// 현재날짜문장
	public static String getNowDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat Date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = Date.format(cal.getTime());
		return dateStr;
	}

	// 파일에 내용쓰기
	public static void writeFileContents(String filePath, int data) {
		writeFileContents(filePath, data + "");
	}

	// 첫 문자 소문자화
	public static String lcfirst(String str) {
		String newStr = "";
		newStr += str.charAt(0);
		newStr = newStr.toLowerCase();

		return newStr + str.substring(1);
	}

	// 파일이 존재하는지
	public static boolean isFileExists(String filePath) {
		File f = new File(filePath);
		if (f.isFile()) {
			return true;
		}

		return false;
	}

	// 파일내용 읽어오기
	public static String getFileContents(String filePath) {
		String rs = null;
		try {
			// 바이트 단위로 파일읽기
			FileInputStream fileStream = null; // 파일 스트림

			fileStream = new FileInputStream(filePath);// 파일 스트림 생성
			// 버퍼 선언
			byte[] readBuffer = new byte[fileStream.available()];
			while (fileStream.read(readBuffer) != -1) {
			}

			rs = new String(readBuffer);

			fileStream.close(); // 스트림 닫기
		} catch (Exception e) {
			e.getStackTrace();
		}

		return rs;
	}

	// 파일 쓰기
	public static void writeFileContents(String filePath, String contents) {
		BufferedOutputStream bs = null;
		try {
			bs = new BufferedOutputStream(new FileOutputStream(filePath));
			bs.write(contents.getBytes()); // Byte형으로만 넣을 수 있음
		} catch (Exception e) {
			e.getStackTrace();
		} finally {
			try {
				bs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Json안에 있는 내용을 가져오기
	public static Object getObjectFromJson(String filePath, Class cls) {
		ObjectMapper om = new ObjectMapper();
		Object obj = null;
		try {
			obj = om.readValue(new File(filePath), cls);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}

		return obj;
	}

	public static void writeJsonFile(String filePath, Object obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(new File(filePath), obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void makeDir(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}