#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
报告生成器
将测试结果生成 Markdown、HTML 和 JSON 格式的报告
"""

import argparse
import json
import sys
import glob
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional

# 添加 lib 目录到 path
SCRIPT_DIR = Path(__file__).parent.absolute()
LIB_DIR = SCRIPT_DIR.parent / "lib"
sys.path.insert(0, str(LIB_DIR))

from utils import log_info, log_success, log_error


class ReportGenerator:
    """报告生成器"""
    
    def __init__(self, results_dir: str):
        self.results_dir = Path(results_dir)
        self.timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    def load_latest_results(self) -> Dict[str, Any]:
        """加载最新的测试结果"""
        results = {
            "scrolling": None,
            "launch": None,
            "switch": None
        }
        
        # 查找最新的汇总文件
        for test_type in ["scrolling", "launch", "switch"]:
            pattern = f"{test_type}_summary_*.json"
            files = sorted(glob.glob(str(self.results_dir / pattern)), reverse=True)
            if files:
                with open(files[0], 'r', encoding='utf-8') as f:
                    results[test_type] = json.load(f)
                log_info(f"加载 {test_type} 测试结果: {files[0]}")
        
        return results

    @staticmethod
    def has_any_results(results: Dict[str, Any]) -> bool:
        """判断是否至少加载到一类有效测试结果"""
        for test_type in ["scrolling", "launch", "switch"]:
            data = results.get(test_type)
            if data and data.get("results"):
                return True
        return False
    
    def generate_markdown(self, results: Dict[str, Any]) -> str:
        """生成 Markdown 格式报告"""
        lines = []
        
        # 标题
        lines.append("# 📊 性能测试报告\n")
        lines.append(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        
        # 设备信息
        device_info = None
        for test_type in ["scrolling", "launch", "switch"]:
            if results.get(test_type) and results[test_type].get("device_info"):
                device_info = results[test_type]["device_info"]
                break
        
        if device_info:
            lines.append("## 📱 设备信息\n")
            lines.append(f"| 属性 | 值 |")
            lines.append(f"|------|-----|")
            lines.append(f"| 品牌 | {device_info.get('brand', 'N/A')} |")
            lines.append(f"| 型号 | {device_info.get('model', 'N/A')} |")
            lines.append(f"| Android 版本 | {device_info.get('android_version', 'N/A')} |")
            lines.append(f"| SDK 版本 | {device_info.get('sdk_version', 'N/A')} |")
            lines.append("")
        
        # 滑动测试结果
        if results.get("scrolling") and results["scrolling"].get("results"):
            lines.append("## 🖱️ 滑动测试结果\n")
            lines.append(f"| 应用 | 负载类型 | 总帧数 | Janky% | 评分 |")
            lines.append(f"|------|----------|--------|--------|------|")
            
            for r in results["scrolling"]["results"]:
                if r.get("status") == "success" and r.get("fps_data"):
                    fps = r["fps_data"]
                    lines.append(
                        f"| {r.get('app_name', 'N/A')} | "
                        f"{r.get('load_type', 'default')} | "
                        f"{fps.get('total_frames', 0)} | "
                        f"{fps.get('janky_percent', 0):.1f}% | "
                        f"{fps.get('grade', 'N/A')} |"
                    )
                elif r.get("status") == "completed":
                    janky_data = r.get("janky_percent", {})
                    if isinstance(janky_data, dict):
                        janky_percent = float(janky_data.get("avg", 0))
                    else:
                        janky_percent = float(janky_data or 0)

                    lines.append(
                        f"| {r.get('app_name', 'N/A')} | "
                        f"{r.get('load_type', 'default')} | "
                        f"N/A | "
                        f"{janky_percent:.1f}% | "
                        f"{r.get('grade', 'N/A')} |"
                    )
            lines.append("")
        
        # 启动测试结果
        if results.get("launch") and results["launch"].get("results"):
            lines.append("## 🚀 启动测试结果\n")
            lines.append(f"| 应用 | Flavor | 平均耗时(ms) | 最小 | 最大 | 评分 |")
            lines.append(f"|------|--------|--------------|------|------|------|")
            
            for r in results["launch"]["results"]:
                if r.get("status") == "completed" and r.get("total_time"):
                    tt = r["total_time"]
                    lines.append(
                        f"| {r.get('app_name', 'N/A')} | "
                        f"{r.get('flavor', 'N/A')} | "
                        f"{tt.get('avg', 0):.0f} | "
                        f"{tt.get('min', 0)} | "
                        f"{tt.get('max', 0)} | "
                        f"{r.get('grade', 'N/A')} |"
                    )
            lines.append("")
        
        # Switch 测试结果
        if results.get("switch") and results["switch"].get("results"):
            lines.append("## 🔄 Switch 跳转测试结果\n")
            lines.append(f"| 应用 | 负载组合 | 平均耗时(ms) | 最小 | 最大 | 评分 |")
            lines.append(f"|------|----------|--------------|------|------|------|")
            
            for r in results["switch"]["results"]:
                if r.get("status") == "completed" and r.get("switch_time"):
                    st = r["switch_time"]
                    lines.append(
                        f"| {r.get('app_name', 'N/A')} | "
                        f"{r.get('load_combination', 'N/A')} | "
                        f"{st.get('avg', 0):.0f} | "
                        f"{st.get('min', 0)} | "
                        f"{st.get('max', 0)} | "
                        f"{r.get('grade', 'N/A')} |"
                    )
            lines.append("")
        
        # 综合评分
        lines.append("## 📈 综合评分\n")
        overall_grade = self._calculate_overall_grade(results)
        lines.append(f"**综合评分: {overall_grade}**\n")
        
        # 评分说明
        lines.append("### 评分标准\n")
        lines.append("| 等级 | 说明 |")
        lines.append("|------|------|")
        lines.append("| A+ | 优秀 - 性能表现极佳 |")
        lines.append("| A  | 良好 - 性能表现良好 |")
        lines.append("| B  | 可接受 - 性能表现一般 |")
        lines.append("| C  | 较差 - 需要优化 |")
        lines.append("| D  | 很差 - 严重性能问题 |")
        
        return "\n".join(lines)
    
    def _calculate_overall_grade(self, results: Dict[str, Any]) -> str:
        """计算综合评分"""
        grades = []
        grade_values = {"A+": 5, "A": 4, "B": 3, "C": 2, "D": 1}
        
        for test_type in ["scrolling", "launch", "switch"]:
            if results.get(test_type) and results[test_type].get("results"):
                for r in results[test_type]["results"]:
                    grade = r.get("grade") or (r.get("fps_data", {}).get("grade"))
                    if grade and grade in grade_values:
                        grades.append(grade_values[grade])
        
        if not grades:
            return "N/A"
        
        avg_value = sum(grades) / len(grades)
        
        if avg_value >= 4.5:
            return "A+"
        elif avg_value >= 3.5:
            return "A"
        elif avg_value >= 2.5:
            return "B"
        elif avg_value >= 1.5:
            return "C"
        else:
            return "D"
    
    def generate_html(self, results: Dict[str, Any]) -> str:
        """生成 HTML 格式报告"""
        markdown_content = self.generate_markdown(results)
        
        # 简单的 Markdown to HTML 转换
        html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>性能测试报告</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        h1 {{ color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }}
        h2 {{ color: #555; margin-top: 30px; }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
        }}
        th, td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        th {{
            background-color: #007bff;
            color: white;
        }}
        tr:nth-child(even) {{ background-color: #f9f9f9; }}
        tr:hover {{ background-color: #f1f1f1; }}
        .grade-A-plus {{ color: #28a745; font-weight: bold; }}
        .grade-A {{ color: #28a745; }}
        .grade-B {{ color: #ffc107; }}
        .grade-C {{ color: #fd7e14; }}
        .grade-D {{ color: #dc3545; }}
    </style>
</head>
<body>
    <div class="container">
        <pre id="markdown-content" style="display:none;">{markdown_content}</pre>
        <div id="html-content"></div>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <script>
        document.getElementById('html-content').innerHTML = 
            marked.parse(document.getElementById('markdown-content').textContent);
    </script>
</body>
</html>
"""
        return html_content
    
    def generate_json(self, results: Dict[str, Any]) -> str:
        """生成 JSON 格式报告"""
        report = {
            "version": "1.0",
            "generated_at": datetime.now().isoformat(),
            "overall_grade": self._calculate_overall_grade(results),
            "scrolling_tests": results.get("scrolling"),
            "launch_tests": results.get("launch"),
            "switch_tests": results.get("switch")
        }
        return json.dumps(report, indent=2, ensure_ascii=False)

    def generate(self, output_format: str = "all") -> Dict[str, str]:
        """根据指定格式生成报告文件"""
        results = self.load_latest_results()
        if not self.has_any_results(results):
            raise ValueError(f"未在结果目录找到可用 summary: {self.results_dir}")

        output_files = {}

        if output_format in ("markdown", "all"):
            md_content = self.generate_markdown(results)
            md_path = self.results_dir / f"report_{self.timestamp}.md"
            with open(md_path, 'w', encoding='utf-8') as f:
                f.write(md_content)
            output_files["markdown"] = str(md_path)
            log_success(f"生成 Markdown 报告: {md_path}")

        if output_format in ("html", "all"):
            html_content = self.generate_html(results)
            html_path = self.results_dir / f"report_{self.timestamp}.html"
            with open(html_path, 'w', encoding='utf-8') as f:
                f.write(html_content)
            output_files["html"] = str(html_path)
            log_success(f"生成 HTML 报告: {html_path}")

        if output_format in ("json", "all"):
            json_content = self.generate_json(results)
            json_path = self.results_dir / f"report_{self.timestamp}.json"
            with open(json_path, 'w', encoding='utf-8') as f:
                f.write(json_content)
            output_files["json"] = str(json_path)
            log_success(f"生成 JSON 报告: {json_path}")

        return output_files
    
    def generate_all(self) -> Dict[str, str]:
        """生成所有格式的报告"""
        return self.generate("all")


def main():
    parser = argparse.ArgumentParser(description="性能测试报告生成器")
    parser.add_argument(
        "--format",
        choices=["markdown", "html", "json", "all"],
        default="all",
        help="输出格式（默认: all）"
    )
    parser.add_argument(
        "--results-dir",
        help="测试结果目录（默认: ../results）"
    )
    
    args = parser.parse_args()
    
    # 设置路径
    script_dir = Path(__file__).parent.absolute()
    project_dir = script_dir.parent
    results_dir = args.results_dir or str(project_dir / "results")
    
    # 创建报告生成器
    generator = ReportGenerator(results_dir)
    
    # 生成报告
    log_info("开始生成测试报告...")
    try:
        output_files = generator.generate(args.format)
    except Exception as e:
        log_error(f"报告生成失败: {e}")
        sys.exit(1)
    
    print("\n" + "="*50)
    print("报告生成完成！")
    print("="*50)
    for fmt, path in output_files.items():
        print(f"  {fmt}: {path}")


if __name__ == "__main__":
    main()
