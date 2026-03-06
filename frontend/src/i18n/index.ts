import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

const resources = {
  en: {
    translation: {
      title: 'Ms Ly English',
      submitHomework: 'Homework Submission',
      studentName: 'Student name',
      classGroup: 'Class / Group',
      homeworkTopic: 'Homework topic',
      notes: 'Notes / Comment',
      files: 'Files',
      submit: 'Submit',
      required: 'This field is required',
      fileRequired: 'Please upload at least one file',
      success: 'Homework submitted successfully',
      back: 'Back to submission page',
      login: 'Admin Login',
      username: 'Username',
      password: 'Password',
      status: 'Status',
      search: 'Search student name',
      filters: 'Filters',
      adminSubmissions: 'Submissions',
      logout: 'Logout'
    }
  },
  vi: {
    translation: {
      title: 'Ms Ly English',
      submitHomework: 'Nộp bài tập',
      studentName: 'Tên học sinh',
      classGroup: 'Lớp / Nhóm',
      homeworkTopic: 'Chủ đề bài tập',
      notes: 'Ghi chú / Bình luận',
      files: 'Tệp',
      submit: 'Gửi',
      required: 'Trường này là bắt buộc',
      fileRequired: 'Vui lòng tải lên ít nhất một tệp',
      success: 'Nộp bài tập thành công',
      back: 'Quay lại trang nộp bài',
      login: 'Đăng nhập quản trị',
      username: 'Tên đăng nhập',
      password: 'Mật khẩu',
      status: 'Trạng thái',
      search: 'Tìm theo tên học sinh',
      filters: 'Bộ lọc',
      adminSubmissions: 'Danh sách bài nộp',
      logout: 'Đăng xuất'
    }
  }
}

i18n.use(initReactI18next).init({
  resources,
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }
})

export default i18n
