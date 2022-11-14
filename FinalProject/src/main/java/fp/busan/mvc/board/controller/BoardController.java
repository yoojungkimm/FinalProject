package fp.busan.mvc.board.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import fp.busan.mvc.board.model.vo.Board;
import fp.busan.mvc.board.model.vo.Comment;
import fp.busan.mvc.board.service.BoardService;
import fp.busan.mvc.member.model.vo.Member;
import fp.busan.pr.common.util.PageInfo;



@RequestMapping("/board")
@Controller
public class BoardController {
	
	@Autowired
	private BoardService service;
	
	
	@GetMapping("/list")
	public String list(Model model, @RequestParam Map<String, String> param) {
		int page = 1;
		if(param.containsKey("page") == true) {
			try {
				page = Integer.parseInt(param.get("page"));
			} catch (Exception e) {}
		}
		int count = service.getBoardCount(param);
		PageInfo pageInfo = new PageInfo(page, 10, count, 10);
		List<Board> list = service.getBoardList(pageInfo, param);
		System.out.println(count);
		
		if(param.get("boardCategory") == null) {
			param.put("boardCategory", "1");
		}
		
		model.addAttribute("list",list);
		model.addAttribute("param",param);
		model.addAttribute("pageInfo",pageInfo);
		return "board/list";
	}
	
	@GetMapping("/view")
	public String view(Model model, @RequestParam("b_code") int b_code) {
		
		Board board = service.findByNo(b_code);
		
		int commentcount = service.getCommentByNoCount(b_code);
		List<Comment> comment = service.findCommentByNo(b_code);
		
		if(board == null) {
			return "redirect:error";
		}
		
		System.out.println(board.toString());
		
		model.addAttribute("board", board);
		model.addAttribute("commentcount",commentcount);
		model.addAttribute("comment",comment);
		return "board/view";
		
	}
	
	@GetMapping("/write")
	public String write() {
		return "board/write";
	}
	
	@PostMapping("/write")
	public String writeBoard(Model model,HttpServletRequest request,
						  	@SessionAttribute(name = "loginMember", required = false) Member loginMember,			
							@ModelAttribute Board board,
							@RequestParam("upfile") MultipartFile upfile
							) {
		
		// 파일을 저장하는 로직
		if(upfile != null && upfile.isEmpty() == false) {
			String rootPath = request.getSession().getServletContext().getRealPath("resources");
			String savePath = rootPath + "/upload/board";
			String renameFileName = service.saveFile(upfile, savePath); // 실제 파일 저장하는 코드
			
			if(renameFileName != null) {
				board.setOriginalFileName(upfile.getOriginalFilename());
				board.setRenamedFileName(renameFileName);
			}
		}
		
		int result = service.saveBoard(board);
		
		if(result > 0) {
			model.addAttribute("msg", "게시글 작성이 정상적으로 성공하였습니다.");
			model.addAttribute("location", "/board/list");
			
		}else {
			model.addAttribute("msg", "게시글 작성에 실패하였습니다.");
			model.addAttribute("location", "/board/list");
		}
		
		return "/common/msg";		
	}
	
	@GetMapping("/deleteboard")
	public String deleteBoard(Model model, @RequestParam("b_code") 
				int b_code,HttpServletRequest request) {
		
		String rootPath = request.getSession().getServletContext().getRealPath("resources");
		rootPath = rootPath + "/upload/board"; 
		int result = service.deleteBoard(b_code, rootPath);
		
		if(result > 0) {
			model.addAttribute("msg","게시판 삭제에 성공하였습니다.");
		}else {
			model.addAttribute("msg","게시판 삭제에 실패하였습니다.");
		}		
		model.addAttribute("location","/board/list");								
		return "/common/msg";
	}
		
	@GetMapping("/updateboard")
	public String updateView(Model model, @RequestParam("b_code") int b_code
			            /* , @SessionAttribute(name = "loginMember", required = false) Member loginMember, */) {
		
		Board board = service.findByNo(b_code);
		
		if(board != null) {
			model.addAttribute("board",board);
			return "board/update";
		} else {
			model.addAttribute("msg","잘못된 접근입니다.");
			model.addAttribute("location", "/");
			return "/common/msg";
		}
	}
	
	@PostMapping("/updateboard")
	public String updateBoard(Model model, HttpServletRequest request, 
			@ModelAttribute Board board,
			@RequestParam("reloadFile") MultipartFile reloadFile) {
		
		// 파일을 저장하는 로직
		if(reloadFile != null && reloadFile.isEmpty() == false) {
			String rootPath = request.getSession().getServletContext().getRealPath("resources");
			String savePath = rootPath + "/upload/board";
			
			// 기존에 파일이 있는 경우 -> 기존 파일 삭제요청 필요!!
			if(board.getRenamedFileName() != null) {
				service.deleteFile(savePath + "/" + board.getRenamedFileName());
			}
			
			String renameFileName = service.saveFile(reloadFile, savePath); // 실제 파일 저장하는 코드
			if(renameFileName != null) {
				board.setOriginalFileName(reloadFile.getOriginalFilename());
				board.setRenamedFileName(renameFileName);
			}
		}
				
		int result = service.saveBoard(board);
		
		if(result > 0) {
			model.addAttribute("msg", "게시글 수정이 정상적으로 성공하였습니다.");
			model.addAttribute("location","/board/view?b_code=" + board.getB_code());
		}else {
			model.addAttribute("msg","게시글 수정에 실패하였습니다.");
			model.addAttribute("location","/board/updateboard?b_code=" + board.getB_code());			
		}
		return "/common/msg";
	}
	
	@PostMapping("/writecomment")
	public String writeComment(Model model, @ModelAttribute Comment comment,
			@SessionAttribute(name = "loginMember", required = false) Member loginMember) {
		
		comment.setUser_code(loginMember.getUser_CODE());
		
		int result = service.saveComment(comment);		
		
		if(result > 0) {
			model.addAttribute("msg", "댓글이 등록 되었습니다.");
			model.addAttribute("location", "/board/view?b_code=" + comment.getB_code());
		}else {
			model.addAttribute("msg", "댓글 작성에 실패하였습니다.");
		}		
		return "/common/msg";	
	}
	
	@GetMapping("/deletecomment")
	public String deleteReply(Model model, @RequestParam Map<String, String> param
					) {
			
		
		int result = service.deleteComment(Integer.parseInt(param.get("c_code")));
		String b_codeStr = "/board/view?b_code=" + param.get("b_code");
		
		if(result > 0) {
			model.addAttribute("msg", "댓글 삭제에 성공하였습니다.");
		}else {
			model.addAttribute("msg", "댓글 삭제에 실패하였습니다.");
		}
		model.addAttribute("location", b_codeStr);
		return "/common/msg";
	}
}
