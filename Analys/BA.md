# OpenSupports BA

---

## 参与者目录 / Actor Catalog

| 参与者 Actor      | 说明 Description                          |
| -------------- | --------------------------------------- |
| 访客 Visitor     | 未登录，可浏览,匿名建单,按号查单（受注册/强制登录设置约束）         |
| 客户 Customer    | 已注册并验证的用户                               |
| 监管人 Supervisor | 客户的特殊角色，可查看被监管用户的工单                     |
| 员工 Staff       | L1 客服 / L2 主管 / L3 管理员（角色=级别 level 1-3） |

---

# 1. 用户功能域 / User Account Domain

## UC-01 用户注册 / User Signup

- **参与者 Actors:** 访客
- **主流程 Main Flow:**
   1. 访客提交邮箱,密码（6-200 字符）
   2. 系统校验：邮箱未注册 .

### domain event

POST UserSignUpInfo
User.create().

## UC-02 账户登录 / Account sign in

- **参与者 Actors:** 访客 客户User 员工Staff(L1,L2,L3)

- **前置条件 Preconditions:** 账户存在

- **主流程 Main Flow:**
  
   1. 用户提交邮箱+密码（或 rememberToken+userId）
   2. 系统检查 ,账户未被停用
   3. 系统创建会话：,token
   4. 若勾选"记住我"：创建 30 天一次性 ,SessionCookie,；清除过期 cookie (,login.php:99-101,149-177,)

- **后置条件 Postconditions:** 会话建立；可选 remember cookie

- **业务规则 Rules:** CSRF 参数 ,csrf_userid,/,csrf_token, 必须匹配 (,libs/Controller.php:81-88,)

### domain event

POST AccountSignIn
AccountSignInService.signIn().

## UC-03 个人资料管理 / Edit Profile

- **参与者 Actors:** 客户,员工,管理员
- **主流程 Main Flow:**
   1. 修改邮箱 ,edit-email.php,：拒绝与他人（用户或员工）冲突
   2. 修改密码 ,edit-password.php,：验证旧密码后更新
- **备选/异常:** 新邮箱冲突 → 拒绝并返回错误

PUT AccountEmailAddress
Account.editEmailAdress().

PUT Account{id=}password 
Account.editPassword(). 

---

# 2. 工单域 / Ticket Domain

## UC-04 创建工单 / Create Ticket

- **参与者 Actors:** 客户
- **前置条件 Preconditions:** 强制登录关闭时访客可建单；否则需登录；部门必须公开（员工例外）
- **主流程 Main Flow:**
   1. 选择部门(不可以选择软件默认部门,默认部门对用户隐藏且不可见),提交标题,内容,附件(不超过一定大小的文件
   2. 保存工单：,unread=false, unreadStaff=true, closed=false,；作者加入工单；非员工作者 ,tickets++,
   3. 数据库生成ticket code

### Domain Event

POST ticket
Ticket.creat().
Repo.save(). 

## UC-05 工单粗略视图 / Tickets Views

- **参与者 Actors:** 用户, 员工(L1 , L2 , L3)
- **主流程 Main Flow:**
   1. 我的工单 ,get-tickets.php,：分配给我的工单（closed/department 过滤,分页）
   2. 我的工单 , 用户发出的全部工单,按照时间排序分页 
   3. 新工单 ,get-new-tickets.php,：本部门**未分配**的开放工单，,unread_staff DESC, 排序
   4. 全部工单 ,get-all-tickets.php,：本部门全部工单（标题搜索 + closed 过滤）
   5. 简单搜索 ,search-tickets.php,：本部门内标题搜索
- **业务规则 Rules:** 所有视图均受部门范围约束

### Domain Event

GET Tickets(I assigned)
TicketSearchRepo.search().

GET Tickets(I sent)
TicketSesrchRepo.search().

GET Tickets(not assigned)
TicketSearchRepo.sesrch().

GET Tickets(all)
TicketSearchRepo.search().

GET Ticket(q=tittle)
TicketSearchRepo.search().

## UC-06 工单具体视图 / Ticket View

- **参与者 Actors:** User ,Staff
- **主流程 Main Flow:**
   1. 用户请求 ,POST /ticket/get, (,get.php:44-57,)
   2. 系统校验访问权：
       - 用户 : 只能访问自己投递的工单
       - 员工 : L1只能访问本部门的工单 , 对是否assign to me 没有限制 .
- **后置条件 Postconditions:** 未读标记更新

### Domain Event

GET DetailedTicket 
TicketRepo.getTicket().

## UC-07 回复工单 / Comment on Ticket

- **参与者 Actors:** Staff  , User
- **主流程 Main Flow:**
   1. 提交信息 , 内容与附件 , Staff回复时可选择“私密”
   2. 系统校验 ,canManageTicket, (,comment.php:80-82,)
   3. 保存提交
   4. 更新未读标记

### Domain Event

POST commentOnTicket
Ticket.appendComment().

## UC-08 编辑工单 / Edit Ticket

- **参与者 Actors:** 客户,员工
- **主流程 Main Flow:**
   1. 编辑标题 ,edit-title.php,：任何 ,canManageTicket, 者，置 ,editedTitle=true,
   2. 编辑评论 ,edit-comment.php,：仅作者本人可编辑，且必须是**最新的** COMMENT 事件 (,edit-comment.php:66-80,)
      ,TICKET_CONTENT_CANNOT_BE_EDITED,
- **备选/异常:** 工单已关闭 → 禁止编辑评论
- **业务规则 Rules:** 私密事件对非员工隐藏 (,Ticket.php:213-215,)

### Domain Event

PUT TicketTiitle
TicketRepo.getTicket().
Ticket.editTittle().

PUT latestComment
TicketRepo.getTicket().
Ticket.editLastComment().

## UC-9 工单标签 / Tags

- **参与者 Actors:** 员工(L1+)

- **主流程 Main Flow:**
  
   1. 添加标签
   2. 删除标签

- **备选/异常:** 重复贴标 → ,TAG_EXISTS,

- **业务规则 Rules:** 备注：分页 OFFSET 硬编码 ,($page-1)*10, 而 LIMIT 用 pageSize —— 潜在不一致 (,search.php:167,)

### Domain Event

POST ArticleTag
Ticket.addTag().

DELETE ArticleTag
Ticket.deleteTag().

## UC-10 工单管理 / Ticket Management

- **参与者 Actors:** Staff(L1~L2) , User(only can close Ticket) , Admin
- **主流程 Main Flow:**
   1. 分配 ,assign-ticket.php, L1,L2等级可以之可以将工单分配给自己 , L3可以将工单分配给任何人 可以跨部门分配 .
   2. 取消分配 , L1, L2 只能解除自己的接受的工单 , L3可以解除任何人跨部门的工单分配 
   3. 变更部门 ,
   4. 关闭 ,
   5. 重开 
   6. 删除 only L3

### domain event

PUT asign Ticket 
Ticket.assignTo().

PUT unAssign Ticket
Ticket.unAssign().

PUT changeDepartment
repo.getTicket().
Ticket.unAssign().
Ticket.changeDepartment().

PUT closeTicket
Ticket.close().

PUT reOpenTicket
Ticket.reOpen().

DELETE deleteTicket() , only Admin
Ticket.delete().

## UC-11 工单高级搜索 / Search

- **参与者 Actors:** 员工(L1+)

- **主流程 Main Flow:**
  
   1. 高级搜索 ,search.php,：过滤集（tittle,tags,ticket code,closed or not,dateRange,departments,authors,owners,assigned or not,query,orderBy,pageSize）

- **业务规则 Rules:** 备注：分页 OFFSET 硬编码 ,($page-1)*10, 而 LIMIT 用 pageSize —— 潜在不一致 (,search.php:167,)

### Domain Event

TicketSearchRepo.advancedSearch().

---

# 3. 员工 用户 治理 / User , Staff  Governance

## UC-12 用户 高级搜索功能 及其治理界面

- **参与者 Actors:** Staff(L1+)
- **主流程 Main Flow:**
   1. 按照 邮箱关键词搜索用户 , 按照注册时间排序 , 按照发出工单数目排序 , range注册时间 ,分页 
   2. 按照时间显示全部用户
   3. 访问用户界面 , 用户基本信息 (emailAddress , signUpTime , 发出的工单)

### Domain Event

GET userSearch
AccountSearchRepo.searchUser().

GET userOverview
AccountRepo.getUser().
UserToDTO
TicketSearchRepo.sesrch().
TicketToDTO

## UC-13 Staff 高级搜索功能

- **参与者 Actors:** Staff(L3)
- **主流程 Main Flow:**
   1. 按照 邮箱关键词搜索Staff,限定部门,range 注册时间 ,Level, 按照注册时间排序, 分页. 
   2. 按照部门显示全部staff
   3. 具体staff界面访问 , staff基本信息 (emailAddress , department , signUptime , level , tickets assigned)

### Domain event

GET StaffSearch
AccountSearchRepo.searchStaff().

GET staffOverview
AccountRepo.getStaff().
StaffToDTO
TIcketSearchRepo.search().
TicketToDTO

## UC-14 员工管理 / Staff Management

- **参与者 Actors:** Admin (L3)
- **主流程 Main Flow:**
   1. 创建新的staff account ,邮箱, pswrd ,level 1-3,department ; 拒绝已存在邮箱
   2. 编辑 , 编辑staff account的 pswrd
   3. 删除 , 不可删自己 ; 全部工单解除分配 .
   4. 停用员工账户,并且解除其全部工单,不可停用自己 ; 启用员工用户
- **业务规则 Rules:** 员工级别 = level 字段 1/2/3，权限 token ,staff_1/2/3, 强制 (,libs/Validator.php:15-27,)

### Domain Event

PUT StaffLevel
StaffAccount.changeLevel().

PUT StaffAppartment
Account.changeAppartment().
PUBLISH EVENT -> AccountAppartmentChanged
TicketSearchRepo.search().
stream().foreach.Ticket.unAssign()

PUT StaffPassword
AccountRepo.getStaff().
Staff.changePassword().

POST StaffAccount()
StaffAccount.create().

DELETE StaffAccount
TicketSearchRepo.search().
stream().foreach.Ticket.unAssign() ,Ticket.close().
PUBLISH EVENT TicketUnAssigned
StaffAccount.delete()

PUT StaffState
StaffRepo.getStaff().
Staff.enable(). / Staff.disable().
PUBLISH EVENT : StaffAccountDisabled
TicketSearchRepo.search().
stream.foreach().Ticket.unAssigned().

## UC-15 用户管理 / User Management

- **参与者 Actors:** Admin (L3)
- **主流程 Main Flow:**
   1. 创建新的useraccount, 拒绝已存在邮箱
   2. 编辑 , 编辑staff account的 pswrd
   3. 删除 , 不可删自己  全部工单解除分配 ->close.
   4. 停用 启用 用户账号

### Domain Event

PUT UserPassword
UserRepo.getUser().
User.overWritePassword().

POST StaffAccount()
StaffAccount.create().

DELETE StaffAccount
TicketSearchRepo.search().
stream().foreach.Ticket.unAssign()  ,Ticket.close().
PUBLISH EVENT TicketUnassigned&Closed  
StaffAccount.delete()

PUT UserState
UserRepo.getUser().
User.disable(). / User.enable().

---

# 4. 知识库 / Knowledge Base

## UC-16 知识库 文章 视图 Knowledge & Article View

- **参与者 Actors:** 访客/客户,员工
- **主流程 Main Flow:**
   1. 浏览知识库概览 ,非员工过滤私有主题
   2. 浏览文章全文

### Domain Event

GET knowledgeBase
ToDTO.toKnowledgeBase(Folder)

GET article
ArticleRepo.getArticle().

## UC-16 知识库概览页面管理 / Knowledge Base View Management

- **参与者 Actors:** ,员工(L2+)（管理）
- **主流程 Main Flow:**
   1. 浏览 ,非员工过滤私有主题
   2. 主题 ,添加主题 ,删除主题(原其下文章进入未分类主题板块下)
   3. 设置主题 "私有" , 解除私有(私有状态下 ,主题连同其板块下article皆不可见) .
   4. 前端拖拽保存分类修改, 拖拽实现文章分类到不同的主题下 . 
- **业务规则 Rules:** 主题:文章 = 1: n , 文章:主题 = 1:1 , 同一个文章不能在两个主题下 , 同一个主题可以拥有多个文章
- ### Domain Event

POST FolderTopic
ArticleRepo.getFolder().
Folder.addTopic().

PUT FolderTopic
ArticleRepo.getFolder().
Folder.editTopic().

DELETE FolderTopic
ArticleRepo.getFolder().
Folder.removeTopic().

PUT FolderOrder
Folder.updateOrder().

PUT TopicState
Folder.setTopicPrivate().

## UC-17 文章管理 / Article Management

- **参与者 Actors:**  ,员工(L2+)（管理）
- **主流程 Main Flow:**
-  1. 创建新的文章 , 
   2. 编辑文章信息(tittle ,正文)
   3. 删除已有文章 .
- **业务规则 Rules:** 主题:文章 = 1: n , 文章:主题 = 1:1 , 同一个文章不能在两个主题下 , 同一个主题可以拥有多个文章
- 

### Domain Event

POST Article
Article.create()
PUBLISH EVENT articleCreated
Folder.addArticle().

PUT Article
ArticleRepo.getArticle().
Article.edit().
PUBLISH EVENT articleUpdated
ArticleRepo.getFolder().
Folder.updateArticleInfo().

DELETE Article
ArticleRepo.getFolder().
Folder.deleteArticle().
PUBLISH EVENT folderArticleDeleted
Article.delete().

---

# 5. Department Overall Management

## UC-18 部门管理 / Department Management

- **参与者 Actors:** 管理员(L3)
- **主流程 Main Flow:**
-  0. 查看  部门概览 .
   1. 添加 ,添加部门,名称唯一
   2. 编辑 ,编辑部门名称
   3. 删除 ,只可以删除其下没有工单和员工的部门 , 不可以删除默认部门 .
   4. 部门迁移 (将该部门的员工 或者 工单 全体迁移到 另一个部门) ,迁移过程中 员工和工单 均解除 assign关系 .
      **业务规则 Rules:** 部门私有标志限制员工/客户可见范围

### Domain Event

GET departments
DepartmentRepo.getDepartmentsOverview().

POST Department
DepartmentService.checkDpNameUnRgst().
Department.create().

PUT DepartmentName
DepartmentService.checkDpNameUnRgst().
Department.reName().

POST DepartmentTicketsMigrate
TicketSearchRepo.search().
Ticket.unAssign().
Ticket.changeDepartment().

PUT DepartmentStaffSMigrate
StaffSearchRepo.search().
TicketSearchRepo.search().
stream.foreach().Ticket.unAssign().
PUBLISH EVENT : ticketsUnassigned
stream.foreach().Staff.changeDepartment().

DELETE department 
Department.delete().

---