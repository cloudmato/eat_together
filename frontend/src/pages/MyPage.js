import { useNavigate } from "react-router-dom";
import MyButton from "../components/MyButton";
import { useEffect, useState } from "react";
import MyHeader from "../components/MyHeader";
import axios from "axios";

const MyPage = () => {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");

  useEffect(() => {
    let isMounted = true;
    const source = axios.CancelToken.source();

    axios
      .get("/user/validate", { cancelToken: source.token })
      .then((res) => {
        if (isMounted && res.status !== 404) {
          const {
            name: userName,
            email: userEmail,
            nickname: userNickname,
          } = res.data;
          setName(userName);
          setEmail(userEmail);
          setNickname(userNickname);
        }
      })
      .catch((error) => {
        if (axios.isCancel(error)) {
          // Request was canceled
          return;
        }
        console.log("Failed to validate user:", error);
        alert("로그인 정보가 없습니다. 다시 로그인해주세요.");
        navigate("/SignIn");
      });

    return () => {
      isMounted = false;
      source.cancel("Request canceled");
    };
  }, [navigate]);

  return (
    <div>
      <MyHeader
        headText={"마이페이지"}
        leftChild={
          <MyButton
            text={"뒤로가기"}
            onClick={() => {
              navigate("/");
            }}
          />
        }
      />
      <h2>마이페이지</h2>
    </div>
  );
};

export default MyPage;
