const API = "http://localhost:8080/api/v1/admin/dot-giam-gia"

export async function getDotGiamGia(pageNo, pageSize, keyword) {
  const res = await fetch(
    `${API}/phan-trang?pageNo=${pageNo}&pageSize=${pageSize}&keyword=${keyword}`
  )
  return res.json()
}

export async function addDotGiamGia(data) {
  return fetch(`${API}/add`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(convertDate(data))
  })
}

export async function updateDotGiamGia(data) {
  return fetch(`${API}/update?id=${data.id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(convertDate(data))
  })
}

export async function deleteDotGiamGia(id) {
  return fetch(`${API}/delete?id=${id}`, {
    method: "DELETE"
  })
}

// 🔥 convert date → timestamp (rất quan trọng)
function convertDate(data) {
  return {
    ...data,
    ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).getTime() : null,
    ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).getTime() : null
  }
}