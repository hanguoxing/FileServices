package xin.hanxx.FileService.controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import xin.hanxx.FileService.domain.UpFile;
import xin.hanxx.FileService.service.UpFileService;
import xin.hanxx.FileService.utils.MD5Util;

/**
 * @ClassName: UpFileController
 * @Description: TODO
 * @author: 韩星星
 * @createTime: 2017年11月9日 下午12:05:48
 * @desc: controller
 */

// 允许所有域名进行访问
@CrossOrigin(origins = "*", maxAge = 3600)
@Controller
public class UpFileController {

	@Autowired
	private UpFileService upFileService;

	// 访问首页 展示所有图库列表
	@RequestMapping(value = "/")
	public String index(Model model) {
		model.addAttribute("files", upFileService.listFiles());
		return "index";
	}

	/**
	 * 获取文件片的信息
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity headerFile(@PathVariable String id) {

		UpFile file = upFileService.getFileById(id);

		if (file != null) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=\"" + file.getName() + "\"")
					.header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
					.header(HttpHeaders.CONTENT_LENGTH, file.getSize() + "").header("Connection", "close")
					.body(file.getContent());
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未发现{" + id + "}的文件");
		}
	}

	/**
	 * 显示图片
	 * 
	 * @param id
	 * @return
	 */
	@GetMapping("/online/{id}")
	@ResponseBody
	public ResponseEntity<?> onlineFile(@PathVariable String id) {
		UpFile file = upFileService.getFileById(id);

		if (file != null) {
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "fileName=\"" + file.getName() + "\"")
					.header(HttpHeaders.CONTENT_TYPE, file.getContentType())
					.header(HttpHeaders.CONTENT_LENGTH, file.getSize() + "").header("Connection", "close")
					.body(file.getContent());
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未发现{" + id + "}的文件");
		}
	}

	/**
	 * 文件的上传
	 * 
	 * @param file
	 * @param redirectAttributes
	 * @return
	 */

	@PostMapping("/")
	public String fileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		// 设置md5
		String upFileMd5;
		if (file!=null) {
			//System.out.println(file.getOriginalFilename());
			try {
				upFileMd5 = MD5Util.getMD5(file.getInputStream());
				if (upFileService.getFileByMd5(upFileMd5) != null) {
					redirectAttributes.addFlashAttribute("message",
							file.getOriginalFilename() + " 文件已存在，请勿重复上传;MD5=" + upFileMd5);
					return "redirect:/";
				} else {
					UpFile up = new UpFile(file.getOriginalFilename(), file.getContentType(), upFileMd5, file.getSize(),
							file.getBytes());
					up.setUploadDate(upFileService.sfDate());
					upFileService.saveFile(up);
					redirectAttributes.addFlashAttribute("message", file.getOriginalFilename() + " 上传成功!");
					return "redirect:/";
				}
			} catch (Exception e) {
				e.printStackTrace();
				redirectAttributes.addFlashAttribute("message", file.getOriginalFilename() + " 上传失败");
				return "redirect:/";
			}		
			
		} else {
			redirectAttributes.addFlashAttribute("message", "文件不存在上传失败！！");
			return "redirect:/";
		}
	}

	/**
	 * 访问upload路径得到图片展示在其他页面
	 * @param file
	 * @return
	 */
	@PostMapping("/upload")
	@ResponseBody
	public ResponseEntity<String> fileUpload(@RequestParam("file")MultipartFile file){
		UpFile returnFile=null;
		String upFileMd5;
		if(file!=null) {
			System.out.println(file.getOriginalFilename());
			try {
				upFileMd5 = MD5Util.getMD5(file.getInputStream());
				if(upFileService.getFileByMd5(upFileMd5)!=null) {
					String path="http://localhost:8088/online/"+upFileService.getFileByMd5(upFileMd5).getId();
					return ResponseEntity.status(HttpStatus.OK).body(path);
				}else {
					UpFile up=new UpFile(file.getOriginalFilename(), file.getContentType(), upFileMd5,file.getSize(), file.getBytes());
					up.setUploadDate(upFileService.sfDate());
					returnFile=upFileService.saveFile(up);
					returnFile.setPath("http://localhost:8088/online/"+up.getId());
					returnFile.setContent(null);
					return ResponseEntity.status(HttpStatus.OK).body("http://localhost:8088/online/"+up.getId());
				}
			} catch (Exception e) {
				e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
			}
		}else{
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件不存在上传失败");
		}
	}
	@DeleteMapping("/delete/{id}")
	public String deleteFile(@PathVariable("id") String id) {

		upFileService.removeFile(id);

		return "redirect:/";

	}
}
