import axios from "axios";
import { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import MyHeader from "../components/MyHeader";
import MyButton from "../components/MyButton";

const Write = () => {
  const navigate = useNavigate();
  const { idx } = useParams();
  const location = useLocation();
  const [title, setTitle] = useState("");
  const [contents, setContents] = useState("");
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(true);
  const isEditMode = location.state && location.state.isEditMode;

  const getCurrentDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    const seconds = String(now.getSeconds()).padStart(2, "0");
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  };

  const formatDateForDisplay = (dateString) => {
    const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const weekday = weekdays[date.getDay()];
    return `${year}-${month}-${day} (${weekday})`;
  };

  useEffect(() => {
    // 서버에서 유저 정보를 가져와서 닉네임 고정
    const fetchUserNickname = async () => {
      try {
        const response = await axios.get("/users/validate");
        const { nickname } = response.data;
        setNickname(nickname);
        setLoading(false);
      } catch (error) {
        console.log("유저 정보를 가져오는데 실패했습니다.", error);
        setLoading(false);
      }
    };

    fetchUserNickname();
  }, []);

  useEffect(() => {
    // 수정 모드인 경우 기존 글의 내용을 불러옴
    if (isEditMode && idx) {
      const fetchPost = async () => {
        try {
          const response = await axios.get(`/posts/${idx}`);
          const { title, contents, nickname } = response.data;
          setTitle(title);
          setContents(contents);
          setNickname(nickname);
        } catch (error) {
          console.log("글 불러오기에 실패했습니다.", error);
        }
      };

      fetchPost();
    }
  }, [isEditMode, idx]);

  // useEffect(() => {
  //   // 더미 데이터
  //   const dummyData = {
  //     title: "더미 제목",
  //     contents: "더미 내용",
  //     nickname: "더미 작성자",
  //   };

  //   // 수정 모드인 경우 기존 글의 내용을 불러옴
  //   if (isEditMode) {
  //     // 더미 데이터를 사용하여 값 설정
  //     setTitle(dummyData.title);
  //     setContents(dummyData.contents);
  //   }
  // }, [isEditMode]);

  const handleTitleChange = (e) => {
    setTitle(e.target.value);
  };

  const handleContentsChange = (e) => {
    setContents(e.target.value);
  };

  const handleSubmit = async () => {
    try {
      if (isEditMode) {
        // 글 수정 요청 처리
        const response = await axios.put(`/posts/${idx}`, {
          title,
          contents,
        });
        console.log("글이 성공적으로 수정되었습니다.", response.data);
        alert("글이 성공적으로 수정되었습니다.");
        navigate(`/board/${idx}`);
      } else {
        // 글 작성 요청 처리
        const response = await axios.post("/posts/add", {
          title,
          contents,
        });
        console.log("글이 성공적으로 저장되었습니다.", response.data);
        alert("글이 성공적으로 저장되었습니다.");
        navigate("/LetsDo");
      }
    } catch (error) {
      console.log("글 저장 또는 수정에 실패했습니다.", error);
      alert("글 저장 또는 수정에 실패했습니다..");
      if (isEditMode) {
        navigate(`/board/${idx}`);
      } else {
        navigate("/LetsDo");
      }
    }
  };
  return (
    <div>
      <MyHeader
        headText={isEditMode ? "글 수정" : "글 쓰기"}
        leftChild={
          <MyButton
            text={"뒤로가기"}
            onClick={() => {
              navigate("/");
            }}
          />
        }
      />
      <div className="flex flex-col pt-10 items-center justify-center">
        <div className="p-4 w-1/2 border-4 border-project rounded-lg">
          <div>
            <input
              className="pt-4 px-4 font-bold text-2xl w-full"
              type="text"
              placeholder="제목을 작성하세요!"
              value={title}
              onChange={handleTitleChange}
            />
            <div className="p-4 flex flex-row justify-between border-b-4 border-orange-100">
              <div>
                <input
                  className=" font-semibold text-lg"
                  type="text"
                  value={formatDateForDisplay(getCurrentDateTime())}
                  readOnly
                />
              </div>
              <div>
                <input
                  className=" text-right font-semibold text-lg text-project"
                  type="text"
                  placeholder="닉네임"
                  value={loading ? "" : nickname}
                  readOnly
                />
              </div>
            </div>
          </div>

          <div className=" mt-10 border-4 border-orange-300 rounded-lg">
            <textarea
              className="p-2 w-full h-80 resize-none outline-none"
              placeholder="본문을 입력하세요!"
              maxLength={"100"}
              value={contents}
              onChange={handleContentsChange}
              required
            />
          </div>
          <div className=" pt-6">
            <MyButton
              text={isEditMode ? "수정하기" : "작성하기"}
              onClick={handleSubmit}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Write;
