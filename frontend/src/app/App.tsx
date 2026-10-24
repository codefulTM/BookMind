import { BrowserRouter, Route, Routes } from 'react-router-dom'

import { AppLayout } from '@/app/AppLayout'
import { BookDetailPage } from '@/pages/BookDetailPage'
import { ChatPage } from '@/pages/ChatPage'
import { LibraryPage } from '@/pages/LibraryPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { SearchPage } from '@/pages/SearchPage'
import { SettingsPage } from '@/pages/SettingsPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<LibraryPage />} />
          <Route path="books/:id" element={<BookDetailPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="chat/:id" element={<ChatPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
