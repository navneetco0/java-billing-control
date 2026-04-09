## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
# java-billing-control


#icon generate
iconutil -c icns MyIcon.iconset

#connect with docker
docker exec -it oracle23ai sqlplus system/PassTheWord@FREE


# Employee Details Table
//  *   CREATE TABLE employee_details (
//  *       emp_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
//  *       emp_name   VARCHAR2(150) NOT NULL,
//  *       gender     VARCHAR2(10),
//  *       dob        VARCHAR2(20),
//  *       doj        VARCHAR2(20),
//  *       phone      VARCHAR2(20),
//  *       mobile_no  VARCHAR2(20) NOT NULL,
//  *       email_id   VARCHAR2(150),
//  *       bsalary    NUMBER(12,2) DEFAULT 0,
//  *       address    VARCHAR2(300),
//  *       narration  VARCHAR2(500),
//  *       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//  *       CONSTRAINT chk_emp_gender_val CHECK (gender IN ('Male','Female','Other'))
//  *   );

# Employee Attendance Table
CREATE TABLE employee_attendance (
    att_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- Change this to NUMBER to match employee_details.emp_id
    emp_id   NUMBER NOT NULL, 
    att_date DATE NOT NULL,
    status   VARCHAR2(20) DEFAULT 'Absent' NOT NULL,
    remarks  VARCHAR2(255) DEFAULT '',
    CONSTRAINT uq_emp_date UNIQUE (emp_id, att_date),
    CONSTRAINT fk_att_emp FOREIGN KEY (emp_id) REFERENCES employee_details(emp_id) ON DELETE CASCADE,
    CONSTRAINT chk_att_status CHECK (status IN ('Present', 'Absent', 'Half-Day', 'Leave'))
);

# Employee Salary Table
