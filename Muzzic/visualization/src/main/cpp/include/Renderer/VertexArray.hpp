#pragma once

#include "Renderer/OpenGL.h"

namespace libprojectM {
namespace Renderer {

class VertexArray
{
public:
    VertexArray()
    {
        glGenVertexArrays(1, &m_vaoID);
    }

    virtual ~VertexArray()
    {
        if (s_currentBoundVao == m_vaoID) {
            s_currentBoundVao = 0;
        }
        glDeleteVertexArrays(1, &m_vaoID);
        m_vaoID = 0;
    }

    void Bind() const
    {
        if (s_currentBoundVao != m_vaoID) {
            glBindVertexArray(m_vaoID);
            s_currentBoundVao = m_vaoID;
        }
    }

    static void Unbind()
    {
        if (s_currentBoundVao != 0) {
            glBindVertexArray(0);
            s_currentBoundVao = 0;
        }
    }

private:
    GLuint m_vaoID{0};
    inline static GLuint s_currentBoundVao{0};
};

} // namespace Renderer
} // namespace libprojectM
